package dev.lioracode.nextbill.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.CancelSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DeleteSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.RestoreSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
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
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import java.io.IOException
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
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val settings = FakeSettingsRepository(UserSettings.DEFAULT)
    private val dates = FakeCurrentDateSource(d("2026-11-15"))
    private val coordinator = DataCoordinator()
    private val appScope = CoroutineScope(main.dispatcher + Job())
    private lateinit var repo: FakeSubscriptionRepository
    private lateinit var cancelUseCase: CancelSubscriptionUseCase
    private lateinit var restoreUseCase: RestoreSubscriptionUseCase
    private lateinit var deleteUseCase: DeleteSubscriptionUseCase

    private val netflix = sub(1, "넷플릭스", "2026-01-15", price = krw(17_000))

    @After fun tearDown() = appScope.cancel()

    private suspend fun TestScope.ready(
        vararg subscriptions: dev.lioracode.nextbill.core.model.Subscription
    ) {
        repo = FakeSubscriptionRepository(subscriptions.toList())
        val sync = SyncRemindersUseCase(
            repo,
            FakeScheduledReminderRepository(),
            FakeReminderAlarm(),
            MutableClock()
        )
        val refresher = DerivedStateRefresher(
            coordinator,
            sync,
            FakeReminderPoster(),
            FakeWidgetUpdater(),
            FakeDayChangeScheduler(),
            dates,
            appScope
        )
        AppStartup(
            coordinator,
            repo,
            settings,
            FakeBackupCodec(),
            FakeImportStaging(),
            sync,
            refresher
        ).start()
        advanceUntilIdle()
        cancelUseCase = CancelSubscriptionUseCase(repo, coordinator, refresher, dates)
        restoreUseCase = RestoreSubscriptionUseCase(repo, coordinator, refresher, dates)
        deleteUseCase = DeleteSubscriptionUseCase(repo, coordinator, refresher)
    }

    private fun vm(id: Long = 1, handle: SavedStateHandle = SavedStateHandle()) = DetailViewModel(
        DetailNavKey(id),
        handle,
        repo,
        dates,
        settings,
        cancelUseCase,
        restoreUseCase,
        deleteUseCase,
        coordinator
    )

    private val DetailViewModel.content: DetailUiState.Content
        get() = uiState.value as DetailUiState.Content

    @Test
    fun showsNextThreeDates() = runTest(main.dispatcher) {
        ready(netflix)
        vm().uiState.test {
            val content = expectMostRecentItem() as DetailUiState.Content
            assertThat(content.mode).isEqualTo(DetailMode.Active)
            assertThat(content.upcoming).containsExactly(
                d("2026-11-15") to DueLabel.Today,
                d("2026-12-15") to DueLabel.InDays(30),
                d("2027-01-15") to DueLabel.InDays(61)
            ).inOrder()
        }
    }

    @Test
    fun equivalents() = runTest(main.dispatcher) {
        ready(netflix)
        vm().uiState.test {
            val content = expectMostRecentItem() as DetailUiState.Content
            assertThat(content.monthly).isEqualTo(krw(17_000))
            assertThat(content.yearly).isEqualTo(krw(204_000))
        }
    }

    @Test
    fun cancelEndsInClosed() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            vm.cancel()
            // 저장소가 먼저 CANCELLED를 내보내도 읽기 전용 화면으로 깜빡이지 않고 바로 Closed.
            assertThat(expectMostRecentItem()).isEqualTo(DetailUiState.Closed)
        }
        assertThat(repo.get(netflix.id)!!.cancelledAt).isEqualTo(d("2026-11-15"))
    }

    @Test
    fun deleteEndsInClosedOnce() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            vm.delete()
            // 작업 중(busy) 상태 다음에 Closed가 한 번.
            assertThat(expectMostRecentItem()).isEqualTo(DetailUiState.Closed)
            expectNoEvents()
            advanceUntilIdle()
            expectNoEvents()
        }
        assertThat(repo.deleteCount).isEqualTo(1)
    }

    @Test
    fun deletedElsewhereCloses() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            repo.delete(netflix.id)
            assertThat(awaitItem()).isEqualTo(DetailUiState.Closed)
            // 끝 상태: 같은 id로 다시 생겨도 무시합니다.
            repo.upsert(netflix)
            expectNoEvents()
        }
    }

    @Test
    fun missingSubscriptionStartsClosed() = runTest(main.dispatcher) {
        ready(netflix)
        vm(id = 99).uiState.test {
            assertThat(expectMostRecentItem()).isEqualTo(DetailUiState.Closed)
        }
    }

    @Test
    fun cancelledElsewhereShowsReadOnly() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            repo.upsert(netflix.cancelledOn(d("2026-11-15")))
            val content = awaitItem() as DetailUiState.Content
            assertThat(content.mode).isEqualTo(DetailMode.Cancelled(d("2026-11-15")))
            assertThat(content.upcoming).isEmpty()
        }
    }

    @Test
    fun beforeFirstBillingShowsDeleteOnly() = runTest(main.dispatcher) {
        ready(sub(1, "새 구독", "2026-12-01"))
        dates.set(d("2026-10-24"))
        vm().uiState.test {
            val content = expectMostRecentItem() as DetailUiState.Content
            assertThat(content.mode).isEqualTo(DetailMode.BeforeFirstBilling)
        }
    }

    @Test
    fun restoreFromReadOnly() = runTest(main.dispatcher) {
        ready(netflix.cancelledOn(d("2026-11-10")))
        val vm = vm()
        vm.uiState.test {
            assertThat((expectMostRecentItem() as DetailUiState.Content).mode)
                .isEqualTo(DetailMode.Cancelled(d("2026-11-10")))
            vm.restore()
            val content = expectMostRecentItem() as DetailUiState.Content
            assertThat(content.mode).isEqualTo(DetailMode.Active)
            assertThat(content.busy).isFalse()
            assertThat(content.upcoming).isNotEmpty()
        }
    }

    @Test
    fun storageFailureReleasesBusy() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            repo.failNextDelete = IOException("disk")
            vm.delete()
            val content = expectMostRecentItem() as DetailUiState.Content
            assertThat(content.busy).isFalse()
            assertThat(content.message).isEqualTo(DetailMessage.STORAGE_FAILED)
            vm.messageShown()
            assertThat((awaitItem() as DetailUiState.Content).message).isNull()
        }
    }

    @Test
    fun staleGenerationClosesOnAction() = runTest(main.dispatcher) {
        ready(netflix)
        // 화면을 연 뒤 데이터 세대가 바뀐 상황: 복원된 세대(-1)가 지금 세대와 다릅니다.
        val vm = vm(handle = SavedStateHandle(mapOf("generation" to -1L)))
        vm.uiState.test {
            assertThat(expectMostRecentItem()).isInstanceOf(DetailUiState.Content::class.java)
            vm.delete()
            assertThat(expectMostRecentItem()).isEqualTo(DetailUiState.Closed)
        }
        assertThat(repo.deleteCount).isEqualTo(0)
    }

    @Test
    fun generationIsStoredOnceAndNotOverwritten() = runTest(main.dispatcher) {
        ready(netflix)
        val handle = SavedStateHandle()
        vm(handle = handle)
        val first = handle.get<Long>("generation")
        assertThat(first).isEqualTo(coordinator.generation.value)
        handle["generation"] = first!! - 1
        vm(handle = handle)
        assertThat(handle.get<Long>("generation")).isEqualTo(first - 1)
    }

    @Test
    fun dateChangeShiftsUpcoming() = runTest(main.dispatcher) {
        ready(netflix)
        val vm = vm()
        vm.uiState.test {
            assertThat((expectMostRecentItem() as DetailUiState.Content).upcoming.first().first)
                .isEqualTo(d("2026-11-15"))
            dates.set(d("2026-11-16"))
            assertThat((awaitItem() as DetailUiState.Content).upcoming.first().first)
                .isEqualTo(d("2026-12-15"))
        }
    }
}
