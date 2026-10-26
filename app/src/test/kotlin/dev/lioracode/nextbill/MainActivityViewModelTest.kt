package dev.lioracode.nextbill

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.model.ThemeMode
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
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainActivityViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository(
        UserSettings.DEFAULT.copy(themeMode = ThemeMode.DARK, useDynamicColor = true)
    )
    private val repo = FakeSubscriptionRepository()

    private fun TestScope.startup(): AppStartup {
        // 앱 범위 scope 대용: 같은 스케줄러, 테스트 Job과 분리된 Job.
        val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val coordinator = DataCoordinator()
        val sync =
            SyncRemindersUseCase(
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
            FakeCurrentDateSource(LocalDate.parse("2026-11-10")),
            FailureReporter.None,
            appScope
        )
        return AppStartup(
            coordinator,
            repo,
            settings,
            FakeBackupCodec(),
            FakeImportStaging(),
            sync,
            refresher,
            FailureReporter.None
        )
    }

    private fun TestScope.viewModel(startup: AppStartup) = MainActivityViewModel(
        settings,
        startup,
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
    )

    @Test
    fun emitsReadyWithSettings() = runTest(main.dispatcher) {
        val startup = startup()
        val vm = viewModel(startup)

        startup.start()
        advanceUntilIdle()

        assertThat(
            vm.uiState.value
        ).isEqualTo(MainUiState.Ready(ThemeMode.DARK, useDynamicColor = true))
    }

    @Test
    fun staysLoadingWhileRecovering() = runTest(main.dispatcher) {
        val startup = startup()
        val vm = viewModel(startup)
        advanceUntilIdle()

        // 설정은 읽었지만 시작 상태가 아직 Recovering입니다.
        assertThat(vm.uiState.value).isEqualTo(MainUiState.Loading)

        startup.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(MainUiState.Ready::class.java)
    }

    @Test
    fun recoveryFailedShowsRecoveryScreen() = runTest(main.dispatcher) {
        val startup = startup()
        val vm = viewModel(startup)
        repo.failNextRevision = IOException("disk")

        startup.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isEqualTo(MainUiState.RecoveryFailed)
    }

    @Test
    fun retryRecoveryReachesReady() = runTest(main.dispatcher) {
        val startup = startup()
        val vm = viewModel(startup)
        repo.failNextRevision = IOException("disk")
        startup.start()
        advanceUntilIdle()

        vm.retryRecovery()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(MainUiState.Ready::class.java)
    }

    @Test
    fun discardPendingImportReachesReadyAndBumpsGeneration() = runTest(main.dispatcher) {
        val startup = startup()
        val vm = viewModel(startup)
        repo.failNextRevision = IOException("disk")
        startup.start()
        advanceUntilIdle()
        val before = vm.dataGeneration.value

        vm.discardPendingImport()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(MainUiState.Ready::class.java)
        assertThat(vm.dataGeneration.value).isGreaterThan(before)
    }
}
