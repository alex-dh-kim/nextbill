package dev.lioracode.nextbill.feature.archive.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DeleteSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.RestoreSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.FakeBackupCodec
import dev.lioracode.nextbill.core.testing.FakeCurrentDateSource
import dev.lioracode.nextbill.core.testing.FakeDayChangeScheduler
import dev.lioracode.nextbill.core.testing.FakeImportStaging
import dev.lioracode.nextbill.core.testing.FakeReminderAlarm
import dev.lioracode.nextbill.core.testing.FakeReminderPoster
import dev.lioracode.nextbill.core.testing.FakeScheduledReminderRepository
import dev.lioracode.nextbill.core.testing.FakeSettingsRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.FakeWidgetUpdater
import dev.lioracode.nextbill.core.testing.MainDispatcherRule
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val settings = FakeSettingsRepository(UserSettings.DEFAULT)
    private val dates = FakeCurrentDateSource(d("2026-11-15"))
    private val coordinator = DataCoordinator()
    private val appScope = CoroutineScope(main.dispatcher + Job())
    private lateinit var repo: FakeSubscriptionRepository
    private lateinit var restoreUseCase: RestoreSubscriptionUseCase
    private lateinit var deleteUseCase: DeleteSubscriptionUseCase

    private val netflix = sub(1, "넷플릭스", "2026-01-15").cancelledOn(d("2026-03-02"))
    private val watcha = sub(2, "왓챠", "2026-01-12").cancelledOn(d("2026-05-20"))
    private val millie = sub(3, "밀리의 서재", "2026-01-14").cancelledOn(d("2026-03-02"))

    @After fun tearDown() = appScope.cancel()

    private suspend fun TestScope.ready(vararg subscriptions: Subscription) {
        repo = FakeSubscriptionRepository(subscriptions.toList())
        val sync = SyncRemindersUseCase(
            repo,
            FakeScheduledReminderRepository(),
            FakeReminderAlarm(),
            MutableClock()
        )
        val refresher = DerivedStateRefresher(
            coordinator,
            repo,
            sync,
            FakeReminderPoster(),
            FakeWidgetUpdater(),
            FakeDayChangeScheduler(),
            dates,
            FailureReporter.None,
            appScope
        )
        AppStartup(
            coordinator,
            repo,
            settings,
            FakeBackupCodec(),
            FakeImportStaging(),
            sync,
            refresher,
            FailureReporter.None
        ).start()
        advanceUntilIdle()
        restoreUseCase = RestoreSubscriptionUseCase(repo, coordinator, refresher, dates)
        deleteUseCase = DeleteSubscriptionUseCase(repo, coordinator, refresher)
    }

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = ArchiveViewModel(
        handle,
        repo,
        restoreUseCase,
        deleteUseCase,
        coordinator
    )

    private val ArchiveViewModel.ready: ArchiveUiState.Ready
        get() = uiState.value as ArchiveUiState.Ready

    @Test
    fun listsCancelledNewestFirst() = runTest(main.dispatcher) {
        ready(netflix, watcha, millie, sub(4, "활성", "2026-01-01"))
        vm().uiState.test {
            val ready = expectMostRecentItem() as ArchiveUiState.Ready
            // 해지일 최신순, 같은 날이면 id 순. 활성 구독은 제외.
            assertThat(ready.items.map { it.id.value }).containsExactly(2L, 1L, 3L).inOrder()
            assertThat(ready.message).isNull()
        }
    }

    @Test
    fun restoreMovesBackToActiveWithNewPeriod() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).items).hasSize(1)
            vm.restore(netflix.id)
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).items).isEmpty()
        }
        val restored = repo.get(netflix.id)!!
        assertThat(restored.cancelledAt).isNull()
        assertThat(restored.periods.last()).isEqualTo(ActivePeriod(d("2026-11-15"), null))
        assertThat(restored.periods).hasSize(2)
    }

    @Test
    fun restoreShowsMessageOnce() = runTest(main.dispatcher) {
        ready(netflix, watcha)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            vm.restore(netflix.id)
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).message)
                .isEqualTo(ArchiveMessage.RESTORED)
            vm.messageShown()
            assertThat((awaitItem() as ArchiveUiState.Ready).message).isNull()
            vm.messageShown()
            expectNoEvents()
        }
    }

    @Test
    fun restoreAtLimitShowsMessage() = runTest(main.dispatcher) {
        val periods = (0 until MAX_PERIODS).map {
            val start = d("2025-01-01").plusDays(it * 2L)
            ActivePeriod(start, start)
        }
        val full = sub(1, "넷플릭스", "2025-01-01").copy(periods = periods)
        ready(full)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            vm.restore(full.id)
            val ready = expectMostRecentItem() as ArchiveUiState.Ready
            assertThat(ready.message).isEqualTo(ArchiveMessage.PERIOD_LIMIT)
            assertThat(ready.items).hasSize(1)
        }
    }

    @Test
    fun doubleRestoreWritesOnce() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            repo.callDelay = 1.seconds
            val before = repo.upsertCount
            vm.restore(netflix.id)
            vm.restore(netflix.id)
            advanceUntilIdle()
            assertThat(repo.upsertCount - before).isEqualTo(1)
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).message)
                .isEqualTo(ArchiveMessage.RESTORED)
        }
    }

    @Test
    fun deleteRemoves() = runTest(main.dispatcher) {
        ready(netflix, watcha)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            vm.delete(netflix.id)
            val ready = expectMostRecentItem() as ArchiveUiState.Ready
            assertThat(ready.items.map { it.id.value }).containsExactly(2L)
            assertThat(ready.message).isNull()
        }
        assertThat(repo.get(netflix.id)).isNull()
    }

    @Test
    fun storageFailureShowsMessage() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            repo.failNextDelete = IOException("disk")
            vm.delete(netflix.id)
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).message)
                .isEqualTo(ArchiveMessage.STORAGE_FAILED)
        }
    }

    @Test
    fun staleGenerationShowsMissing() = runTest(main.dispatcher) {
        ready(netflix)
        // 화면을 연 뒤 데이터 세대가 바뀐 상황: 복원된 세대(-1)가 지금 세대와 다릅니다.
        val vm = vm(SavedStateHandle(mapOf("generation" to -1L)))
        vm.uiState.test {
            expectMostRecentItem()
            vm.delete(netflix.id)
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).message)
                .isEqualTo(ArchiveMessage.MISSING)
        }
        assertThat(repo.deleteCount).isEqualTo(0)
    }

    @Test
    fun generationIsStoredOnceAndNotOverwritten() = runTest(main.dispatcher) {
        ready(netflix)
        val handle = SavedStateHandle()
        vm(handle)
        val first = handle.get<Long>("generation")
        assertThat(first).isEqualTo(coordinator.generation.value)
        handle["generation"] = first!! - 1
        vm(handle)
        assertThat(handle.get<Long>("generation")).isEqualTo(first - 1)
    }

    @Test
    fun restoringAMissingIdShowsMissing() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            vm.restore(SubscriptionId(99))
            assertThat((expectMostRecentItem() as ArchiveUiState.Ready).message)
                .isEqualTo(ArchiveMessage.MISSING)
        }
    }
}
