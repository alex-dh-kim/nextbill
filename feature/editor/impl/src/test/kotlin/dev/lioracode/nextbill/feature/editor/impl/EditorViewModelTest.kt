package dev.lioracode.nextbill.feature.editor.impl

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.SaveSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.domain.validation.CycleUnit
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.Reminder
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
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey
import java.io.IOException
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds
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
class EditorViewModelTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val customReminder = Reminder(true, 3, LocalTime.of(20, 30))
    private val settings = FakeSettingsRepository(
        UserSettings.DEFAULT.copy(
            defaultCurrency = CurrencyCode.USD,
            defaultReminder = customReminder
        )
    )
    private var repo = FakeSubscriptionRepository()
    private val dates = FakeCurrentDateSource(d("2026-11-15"))
    private val coordinator = DataCoordinator()
    private val appScope = CoroutineScope(main.dispatcher + Job())
    private lateinit var startup: AppStartup
    private lateinit var save: SaveSubscriptionUseCase

    private val needsPermission = NotificationDelivery.NeedsPermission(canAsk = true)
    private val available = NotificationDelivery.Available

    @After fun tearDown() = appScope.cancel()

    /** 저장소를 정한 뒤 앱 시작(Ready)까지 마칩니다. */
    private suspend fun TestScope.ready(repository: FakeSubscriptionRepository = repo) {
        repo = repository
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
        startup = AppStartup(
            coordinator,
            repo,
            settings,
            FakeBackupCodec(),
            FakeImportStaging(),
            sync,
            refresher
        )
        save = SaveSubscriptionUseCase(repo, coordinator, refresher)
        startup.start()
        advanceUntilIdle()
    }

    private fun vm(
        key: EditorNavKey = EditorNavKey(),
        handle: SavedStateHandle = SavedStateHandle()
    ) = EditorViewModel(key, handle, repo, settings, save, dates, coordinator)

    private val EditorViewModel.state: EditorUiState get() = checkNotNull(uiState.value)

    private fun EditorViewModel.fillValid() = update {
        it.copy(name = "넷플릭스", amountText = "17000")
    }

    @Test
    fun newUsesSettingsDefaults() = runTest(main.dispatcher) {
        ready()
        val state = vm().state

        assertThat(state.isNew).isTrue()
        assertThat(state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(state.showErrors).isFalse()
        assertThat(state.draft.currency).isEqualTo(CurrencyCode.USD)
        assertThat(state.draft.reminder).isEqualTo(customReminder)
        assertThat(state.draft.anchorDate).isEqualTo(d("2026-11-15"))
        assertThat(state.draft.colorKey).isEqualTo(ColorKey.BLUE)
        assertThat(state.draft.category).isEqualTo(Category.OTHER)
        assertThat(state.draft.interval).isEqualTo(1)
        assertThat(state.draft.unit).isEqualTo(CycleUnit.MONTHS)
    }

    @Test
    fun editLoadsExisting() = runTest(main.dispatcher) {
        val existing = sub(7, "유튜브", colorKey = ColorKey.PINK)
        ready(FakeSubscriptionRepository(listOf(existing)))

        val state = vm(EditorNavKey(7)).state

        assertThat(state.isNew).isFalse()
        assertThat(state.draft).isEqualTo(existing.toDraft())
        assertThat(state.phase).isEqualTo(EditorPhase.Editing)
    }

    @Test
    fun missingSubscriptionGoesToMissing() = runTest(main.dispatcher) {
        ready()
        assertThat(vm(EditorNavKey(42)).state.phase).isEqualTo(EditorPhase.Missing)
    }

    @Test
    fun saveWithErrorsShowsErrors() = runTest(main.dispatcher) {
        ready()
        val model = vm()

        model.onSaveClick(available)

        val state = model.state
        assertThat(state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(state.showErrors).isTrue()
        assertThat(state.errors).containsExactly(FieldError.NAME_BLANK, FieldError.AMOUNT_BLANK)
        assertThat(repo.getAll()).isEmpty()
    }

    @Test
    fun saveAsksPermissionBeforeSaving() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()

        model.onSaveClick(needsPermission)

        assertThat(model.state.phase).isEqualTo(EditorPhase.AskingPermission(saveAfter = true))
        assertThat(repo.getAll()).isEmpty()

        model.onPermissionRequestResult()
        advanceUntilIdle()

        val phase = model.state.phase
        assertThat(phase).isInstanceOf(EditorPhase.Saved::class.java)
        assertThat(repo.getAll().map { it.id }).containsExactly((phase as EditorPhase.Saved).id)
    }

    @Test
    fun saveWithoutPermissionNeededSavesDirectly() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()

        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.phase).isEqualTo(EditorPhase.Saved(SubscriptionId(1)))
        assertThat(repo.getAll()).hasSize(1)
    }

    @Test
    fun reminderOffSavesWithoutAsking() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.update {
            it.copy(name = "넷플릭스", amountText = "17", reminder = it.reminder.copy(enabled = false))
        }

        model.onSaveClick(needsPermission)
        advanceUntilIdle()

        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
    }

    @Test
    fun deniedPermissionStillSavesWithReminderIntent() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()

        model.onSaveClick(needsPermission)
        model.onPermissionRequestResult() // 사용자가 거부해도 결과 콜백은 같습니다.
        advanceUntilIdle()

        assertThat(repo.getAll().single().reminder.enabled).isTrue()
    }

    @Test
    fun switchAsksWithoutSaving() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.update { it.copy(reminder = it.reminder.copy(enabled = false)) }

        model.onReminderSwitch(true, needsPermission)

        assertThat(model.state.draft.reminder.enabled).isTrue()
        assertThat(model.state.phase).isEqualTo(EditorPhase.AskingPermission(saveAfter = false))
        model.onPermissionLater()
        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(model.state.draft.reminder.enabled).isTrue()
        assertThat(repo.getAll()).isEmpty()
    }

    @Test
    fun permissionNotAskedTwiceInSameSession() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.update { it.copy(reminder = it.reminder.copy(enabled = false)) }
        model.onReminderSwitch(true, needsPermission)
        model.onPermissionLater()
        model.fillValid()

        model.onSaveClick(needsPermission)
        advanceUntilIdle()

        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
    }

    @Test
    fun askedFlagSurvivesRestore() = runTest(main.dispatcher) {
        ready()
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        first.fillValid()
        first.onReminderSwitch(true, needsPermission)
        first.onPermissionLater()

        val restored =
            vm(handle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        restored.onSaveClick(needsPermission)
        advanceUntilIdle()

        assertThat(restored.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
    }

    @Test
    fun laterDoesNotMarkRequested() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()
        model.onSaveClick(needsPermission)

        model.onPermissionLater()
        advanceUntilIdle()

        assertThat(settings.notificationPermissionRequested.first()).isFalse()
        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
    }

    @Test
    fun systemRequestMarksRequested() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.onReminderSwitch(true, needsPermission)

        model.onPermissionRequestResult()
        advanceUntilIdle()

        assertThat(settings.notificationPermissionRequested.first()).isTrue()
        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
    }

    @Test
    fun storageFailureKeepsDraftAndAllowsRetry() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()
        val draft = model.state.draft
        repo.failNextUpsert = IOException("disk full")

        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(model.state.message).isEqualTo(EditorMessage.STORAGE_FAILED)
        assertThat(model.state.draft).isEqualTo(draft)
        model.messageShown()
        assertThat(model.state.message).isNull()

        model.onSaveClick(available)
        advanceUntilIdle()
        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
    }

    @Test
    fun limitReachedShowsMessage() = runTest(main.dispatcher) {
        ready(FakeSubscriptionRepository((1L..MAX_SUBSCRIPTIONS).map { sub(it) }))
        val model = vm()
        model.fillValid()

        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(model.state.message).isEqualTo(EditorMessage.LIMIT_REACHED)
        assertThat(repo.count()).isEqualTo(MAX_SUBSCRIPTIONS)
    }

    @Test
    fun notReadyShowsMessageAndKeepsDraft() = runTest(main.dispatcher) {
        // 앱 시작(Recovering → Ready)을 마치지 않은 상태.
        save = SaveSubscriptionUseCase(
            repo,
            coordinator,
            DerivedStateRefresher(
                coordinator,
                SyncRemindersUseCase(
                    repo,
                    FakeScheduledReminderRepository(),
                    FakeReminderAlarm(),
                    MutableClock()
                ),
                FakeReminderPoster(),
                FakeWidgetUpdater(),
                FakeDayChangeScheduler(),
                dates,
                appScope
            )
        )
        val model = vm()
        model.fillValid()

        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.message).isEqualTo(EditorMessage.NOT_READY)
        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(model.state.draft.name).isEqualTo("넷플릭스")
    }

    @Test
    fun doubleSaveClickSavesOnce() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()
        repo.callDelay = 1.seconds

        model.onSaveClick(available)
        assertThat(model.state.phase).isEqualTo(EditorPhase.Saving)
        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
        assertThat(repo.upsertCount).isEqualTo(1)
        model.onSaveClick(available)
        advanceUntilIdle()
        assertThat(repo.upsertCount).isEqualTo(1)
    }

    @Test
    fun draftRestoresFromSerializedState() = runTest(main.dispatcher) {
        ready()
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        first.update {
            it.copy(
                name = "헬스장 💪",
                amountText = "60,000.5",
                interval = 3,
                unit = CycleUnit.WEEKS,
                anchorDate = d("2025-02-28"),
                category = Category.LIVING,
                colorKey = ColorKey.GREEN,
                memo = "메모\n두 줄",
                reminder = Reminder(false, 7, LocalTime.of(7, 15))
            )
        }
        val json = handle.get<String>("draft")
        assertThat(json).isNotNull()

        val restored = vm(handle = SavedStateHandle(mapOf("draft" to json)))

        assertThat(restored.state.draft).isEqualTo(first.state.draft)
    }

    @Test
    fun editRestoreDoesNotReload() = runTest(main.dispatcher) {
        val existing = sub(7, "유튜브")
        ready(FakeSubscriptionRepository(listOf(existing)))
        val handle = SavedStateHandle()
        vm(EditorNavKey(7), handle).update { it.copy(name = "유튜브 프리미엄") }
        val reads = repo.getCount

        val restored =
            vm(
                EditorNavKey(7),
                SavedStateHandle(
                    handle.keys().associateWith {
                        handle.get<Any>(it)
                    }
                )
            )

        assertThat(restored.state.draft.name).isEqualTo("유튜브 프리미엄")
        assertThat(repo.getCount).isEqualTo(reads)
    }

    @Test
    fun changingDefaultsLaterDoesNotAffectExisting() = runTest(main.dispatcher) {
        val existing = sub(7, "유튜브", reminder = Reminder(true, 0, LocalTime.of(8, 0)))
        ready(FakeSubscriptionRepository(listOf(existing)))
        val editing = vm(EditorNavKey(7))
        val creating = vm()

        settings.update { it.copy(defaultReminder = Reminder(false, 7, LocalTime.of(22, 0))) }
        advanceUntilIdle()

        assertThat(editing.state.draft.reminder).isEqualTo(Reminder(true, 0, LocalTime.of(8, 0)))
        assertThat(creating.state.draft.reminder).isEqualTo(customReminder)
    }

    @Test
    fun importAfterOpeningGoesToMissing() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.fillValid()
        startup.discardPendingImport() // 데이터 세대 +1 (가져오기와 같은 효과)
        advanceUntilIdle()

        model.onSaveClick(available)
        advanceUntilIdle()

        assertThat(model.state.phase).isEqualTo(EditorPhase.Missing)
        assertThat(repo.getAll()).isEmpty()
    }

    @Test
    fun restoredGenerationIsNotOverwritten() = runTest(main.dispatcher) {
        ready()
        val handle = SavedStateHandle()
        vm(handle = handle).fillValid()
        startup.discardPendingImport()
        advanceUntilIdle()

        val restored =
            vm(handle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        restored.onSaveClick(available)
        advanceUntilIdle()

        assertThat(restored.state.phase).isEqualTo(EditorPhase.Missing)
    }

    @Test
    fun tooLongNameBlocksSaveWithFieldError() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.update { it.copy(name = "a".repeat(31), amountText = "1") }

        model.onSaveClick(available)

        assertThat(model.state.errors).containsExactly(FieldError.NAME_TOO_LONG)
        assertThat(model.state.showErrors).isTrue()
        assertThat(model.state.phase).isEqualTo(EditorPhase.Editing)
        assertThat(repo.upsertCount).isEqualTo(0)
    }

    @Test
    fun inputDuringSavingIsIgnored() = runTest(main.dispatcher) {
        ready()
        val model = vm()
        model.update {
            it.copy(
                name = "넷플릭스",
                amountText = "17000.00", // USD(기본 통화)의 toDraft 표기와 같게
                reminder = it.reminder.copy(enabled = false)
            )
        }
        val preSave = model.state.draft
        repo.callDelay = 1.seconds

        model.onSaveClick(available)
        assertThat(model.state.phase).isEqualTo(EditorPhase.Saving)
        model.onReminderSwitch(true, needsPermission)
        model.update { it.copy(name = "바뀐 이름") }
        assertThat(model.state.phase).isEqualTo(EditorPhase.Saving)
        assertThat(model.state.draft).isEqualTo(preSave)
        model.onPermissionLater() // 시트가 없으므로 아무 일도 없어야 합니다.
        model.onSaveClick(available)
        assertThat(model.state.phase).isEqualTo(EditorPhase.Saving)
        advanceUntilIdle()

        assertThat(model.state.phase).isInstanceOf(EditorPhase.Saved::class.java)
        assertThat(repo.upsertCount).isEqualTo(1)
        assertThat(
            repo.getAll().single().toDraft()
        ).isEqualTo(preSave.copy(id = repo.getAll().single().id))
    }
}
