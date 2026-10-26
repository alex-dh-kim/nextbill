package dev.lioracode.nextbill.feature.settings.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.ExportBackupUseCase
import dev.lioracode.nextbill.core.domain.usecase.ImportBackupUseCase
import dev.lioracode.nextbill.core.domain.usecase.StartupState
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.domain.usecase.UpdateSettingsUseCase
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
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
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import dev.lioracode.nextbill.feature.settings.impl.backup.BackupTooLargeException
import java.io.IOException
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val settings = FakeSettingsRepository(UserSettings.DEFAULT)
    private val repo = FakeSubscriptionRepository(listOf(sub(1, "넷플릭스", "2026-01-15")))
    private val codec = FakeBackupCodec()
    private val staging = FakeImportStaging()
    private val coordinator = DataCoordinator()
    private val appScope = CoroutineScope(main.dispatcher + Job())
    private val sync = SyncRemindersUseCase(
        repo,
        FakeScheduledReminderRepository(),
        FakeReminderAlarm(),
        MutableClock()
    )
    private val refresher = DerivedStateRefresher(
        coordinator,
        repo,
        sync,
        FakeReminderPoster(),
        FakeWidgetUpdater(),
        FakeDayChangeScheduler(),
        FakeCurrentDateSource(d("2026-11-15")),
        FailureReporter.None,
        appScope
    )

    @After fun tearDown() = appScope.cancel()

    private suspend fun TestScope.start() {
        AppStartup(
            coordinator,
            repo,
            settings,
            codec,
            staging,
            sync,
            refresher,
            FailureReporter.None
        ).start()
        advanceUntilIdle()
    }

    private fun vm() = SettingsViewModel(
        settingsRepository = settings,
        updateSettings = UpdateSettingsUseCase(settings, coordinator, refresher),
        exportBackup = ExportBackupUseCase(repo, settings, codec, coordinator),
        importBackup = ImportBackupUseCase(
            repo,
            settings,
            codec,
            staging,
            coordinator,
            sync,
            refresher
        ),
        appVersion = "1.0.0",
        io = main.dispatcher,
        dateSource = FakeCurrentDateSource(d("2026-11-15"))
    )

    private val SettingsViewModel.state: SettingsUiState get() = uiState.value!!

    /** uiState를 구독해 둔 채로 [block]을 실행합니다(WhileSubscribed). */
    private suspend fun SettingsViewModel.observing(block: suspend SettingsViewModel.() -> Unit) {
        uiState.test {
            expectMostRecentItem()
            block()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun showsSettingsAndVersion() = runTest(main.dispatcher) {
        start()
        vm().uiState.test {
            val state = expectMostRecentItem()!!
            assertThat(state.settings).isEqualTo(UserSettings.DEFAULT)
            assertThat(state.appVersion).isEqualTo("1.0.0")
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
            assertThat(state.message).isNull()
        }
    }

    @Test
    fun changingDefaultsPersists() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            val reminder = Reminder(true, 3, LocalTime.of(21, 30))
            setDefaultReminder(reminder)
            setTheme(ThemeMode.DARK)
            setDynamicColor(true)
            setDefaultCurrency(CurrencyCode.USD)

            val expected = UserSettings(reminder, ThemeMode.DARK, true, CurrencyCode.USD)
            assertThat(settings.current).isEqualTo(expected)
            assertThat(state.settings).isEqualTo(expected)
            assertThat(state.message).isNull()
        }
    }

    @Test
    fun settingsWriteBeforeReadyShowsNotReady() = runTest(main.dispatcher) {
        val vm = vm()
        vm.observing {
            setTheme(ThemeMode.DARK)
            assertThat(state.message).isEqualTo(SettingsMessage.SettingsNotReady)
            assertThat(settings.current.themeMode).isEqualTo(ThemeMode.SYSTEM)
        }
    }

    @Test
    fun settingsStorageFailureShowsMessage() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            settings.failNextUpdate = IOException("disk")
            setDynamicColor(true)
            assertThat(state.message).isEqualTo(SettingsMessage.SettingsStorageFailed)
            assertThat(settings.current.useDynamicColor).isFalse()
        }
    }

    @Test
    fun importSuccessMessage() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            codec.decodeResult = Outcome.Ok(
                BackupSnapshot(
                    listOf(sub(1, "왓챠", "2026-01-12"), sub(2, "멜론", "2026-02-01")),
                    UserSettings.DEFAULT
                )
            )
            import { "json" }
            assertThat(state.message).isEqualTo(SettingsMessage.Imported(2))
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
            assertThat(codec.lastDecodedInput).isEqualTo("json")
        }
        assertThat(repo.getAll().map { it.name }).containsExactly("왓챠", "멜론")
    }

    @Test
    fun importErrorsMapToMessages() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            fun expect(message: SettingsMessage?) {
                assertThat(state.message).isEqualTo(message)
                assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
                messageShown()
            }

            codec.decodeResult = Outcome.Err(BackupError.Malformed)
            import { "{" }
            expect(SettingsMessage.ImportMalformed)

            codec.decodeResult = Outcome.Err(BackupError.TooLarge)
            import { "{}" }
            expect(SettingsMessage.ImportTooLarge)

            codec.decodeResult = Outcome.Err(BackupError.UnsupportedVersion(2))
            import { "{}" }
            expect(SettingsMessage.ImportUnsupported(2))

            // 읽기에서 한도를 넘으면 디코더까지 가지 않습니다.
            codec.lastDecodedInput = null
            import { throw BackupTooLargeException() }
            expect(SettingsMessage.ImportTooLarge)
            assertThat(codec.lastDecodedInput).isNull()

            import { throw IOException("gone") }
            expect(SettingsMessage.IoFailed)

            // staging 쓰기 실패: 데이터는 그대로입니다.
            codec.decodeResult = Outcome.Ok(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
            staging.failWrite = IOException("full")
            import { "json" }
            expect(SettingsMessage.ImportStagingFailed)
            assertThat(repo.getAll().map { it.name }).containsExactly("넷플릭스")
            staging.failWrite = null
        }
    }

    @Test
    fun importBeforeReadyShowsNotReady() = runTest(main.dispatcher) {
        val vm = vm()
        vm.observing {
            codec.decodeResult = Outcome.Ok(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
            import { "json" }
            assertThat(state.message).isEqualTo(SettingsMessage.ImportNotReady)
        }
    }

    @Test
    fun importApplyFailureShowsNoMessageAndAppIsRecoveryFailed() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            codec.decodeResult = Outcome.Ok(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
            repo.failNextReplaceAll = IOException("disk")
            repo.failNextReplaceAllTimes = 2
            import { "json" }
            assertThat(state.message).isNull()
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
        }
        assertThat(coordinator.state.value).isEqualTo(StartupState.RecoveryFailed)
    }

    @Test
    fun backupButtonsDisabledWhileRunning() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            val reading = CompletableDeferred<String>()
            codec.decodeResult = Outcome.Ok(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
            import { reading.await() }
            assertThat(state.backup).isEqualTo(BackupStatus.IMPORTING)

            val written = mutableListOf<String>()
            export { written += it }
            import { error("second import must be ignored") }
            assertThat(written).isEmpty()
            assertThat(state.backup).isEqualTo(BackupStatus.IMPORTING)

            reading.complete("json")
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
            assertThat(state.message).isEqualTo(SettingsMessage.Imported(0))
        }
    }

    @Test
    fun exportWritesEncoded() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            val written = mutableListOf<String>()
            export { written += it }
            assertThat(written).containsExactly("fake-backup:1")
            assertThat(state.message).isEqualTo(SettingsMessage.Exported)
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
        }
        assertThat(codec.lastEncoded)
            .isEqualTo(BackupSnapshot(repo.getAll(), settings.settings.first()))
    }

    @Test
    fun exportWriteFailureIsNotReportedAsSuccess() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            export { throw IOException("provider died") }
            assertThat(state.message).isEqualTo(SettingsMessage.IoFailed)
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
        }
    }

    /** 제공자의 IllegalArgumentException·IllegalStateException도 IoFailed입니다. 취소는 메시지가 아닙니다. */
    @Test
    fun providerRuntimeFailuresMapToIoFailedButCancellationDoesNot() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            val failures = listOf(
                IllegalArgumentException("unknown uri"),
                IllegalStateException("provider gone"),
                SecurityException("revoked")
            )
            failures.forEach { failure ->
                export { throw failure }
                assertThat(state.message).isEqualTo(SettingsMessage.IoFailed)
                messageShown()
                import { throw failure }
                assertThat(state.message).isEqualTo(SettingsMessage.IoFailed)
                messageShown()
            }

            export { throw CancellationException("closed") }
            assertThat(state.message).isNull()
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
            import { throw CancellationException("closed") }
            assertThat(state.message).isNull()
            assertThat(state.backup).isEqualTo(BackupStatus.IDLE)
        }
    }

    @Test
    fun exportBeforeReadyShowsNotReady() = runTest(main.dispatcher) {
        val vm = vm()
        vm.observing {
            val written = mutableListOf<String>()
            export { written += it }
            assertThat(written).isEmpty()
            assertThat(state.message).isEqualTo(SettingsMessage.ExportNotReady)
        }
    }

    @Test
    fun exportReadFailureShowsStorageFailed() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.observing {
            repo.failNextGetAll = IOException("db")
            val written = mutableListOf<String>()
            export { written += it }
            assertThat(written).isEmpty()
            assertThat(state.message).isEqualTo(SettingsMessage.SettingsStorageFailed)
        }
    }

    @Test
    fun messageClearedAfterShown() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        vm.uiState.test {
            expectMostRecentItem()
            vm.export { }
            assertThat(expectMostRecentItem()!!.message).isEqualTo(SettingsMessage.Exported)
            vm.messageShown()
            assertThat(awaitItem()!!.message).isNull()
            vm.messageShown()
            expectNoEvents()
        }
    }

    @Test
    fun backupFileNameUsesToday() {
        assertThat(vm().backupFileName()).isEqualTo("nextbill-backup-2026-11-15.json")
    }

    @Test
    fun permissionResultIsRecorded() = runTest(main.dispatcher) {
        start()
        val vm = vm()
        assertThat(settings.notificationPermissionRequested.first()).isFalse()
        vm.onPermissionRequestResult()
        assertThat(settings.notificationPermissionRequested.first()).isTrue()
    }
}
