package dev.lioracode.nextbill.feature.editor.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.SaveError
import dev.lioracode.nextbill.core.domain.usecase.SaveSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.runCatchingNonCancellation
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.domain.validation.validate
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 구독 등록·수정(F1·F2·F7). 저장 흐름은 설계 문서 ADR-5를 따릅니다: 알림이 켜진 구독을 저장하기 직전에
 * 권한 안내 시트를 한 번 보여 주고, 거부해도 구독의 알림 의도(`reminder.enabled`)는 바꾸지 않습니다.
 *
 * 화면 이동은 하지 않습니다. 결과는 [EditorPhase]로 내보내고 화면이 그 상태를 보고 닫습니다.
 */
@HiltViewModel(assistedFactory = EditorViewModel.Factory::class)
class EditorViewModel @AssistedInject constructor(
    @Assisted key: EditorNavKey,
    private val handle: SavedStateHandle,
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val save: SaveSubscriptionUseCase,
    private val dateSource: CurrentDateSource,
    coordinator: DataCoordinator
) : ViewModel() {
    private val subscriptionId: Long? = key.subscriptionId
    private val isNew = subscriptionId == null

    /**
     * 화면을 처음 열 때의 데이터 세대. 세대는 영속 값이라 프로세스가 다시 만들어져도 비교 기준이 같으므로,
     * 복원할 때 지금 값으로 덮어쓰지 않습니다(그 사이 가져오기가 있었다면 저장이 Missing이 되어야 함).
     */
    private val generation: Long =
        handle.get<Long>(KEY_GENERATION) ?: coordinator.generation.value.also {
            handle[KEY_GENERATION] = it
        }

    /** 이 화면에서 권한 안내 시트를 이미 보여 줬는지. */
    private var askedThisScreen: Boolean
        get() = handle.get<Boolean>(KEY_ASKED) ?: false
        set(value) {
            handle[KEY_ASKED] = value
        }

    private val state = MutableStateFlow<EditorUiState?>(null)

    /** null = 기존 구독을 읽는 중. */
    val uiState: StateFlow<EditorUiState?> = state.asStateFlow()

    /** OS 권한 대화상자를 띄운 적이 있는지(알림 수신 가능 여부 계산용). */
    val permissionRequestedBefore: StateFlow<Boolean> = settings.notificationPermissionRequested
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        val restored = handle.get<String>(KEY_DRAFT)?.let(DraftSaver::decode)
        if (restored != null) {
            state.value =
                editing(restored, showErrors = handle.get<Boolean>(KEY_SHOW_ERRORS) ?: false)
        } else {
            viewModelScope.launch { load() }
        }
    }

    private suspend fun load() {
        val draft = if (subscriptionId == null) {
            val s = settings.settings.first()
            SubscriptionDraft(
                currency = s.defaultCurrency,
                anchorDate = dateSource.today.value,
                reminder = s.defaultReminder
            )
        } else {
            runCatchingNonCancellation { repo.get(SubscriptionId(subscriptionId)) }
                .getOrNull()
                ?.toDraft()
        }
        if (draft == null) {
            state.value = EditorUiState(
                isNew = false,
                draft = SubscriptionDraft(
                    currency = settings.settings.first().defaultCurrency,
                    anchorDate = dateSource.today.value,
                    reminder = Reminder.DEFAULT
                ),
                errors = emptySet(),
                showErrors = false,
                phase = EditorPhase.Missing,
                message = null
            )
        } else {
            persist(draft)
            state.value = editing(draft, showErrors = false)
        }
    }

    /** 편집 중(Editing)일 때만 반영합니다. 저장 중의 입력은 저장할 값과 어긋나므로 받지 않습니다. */
    fun update(transform: (SubscriptionDraft) -> SubscriptionDraft) {
        val current = state.value ?: return
        if (current.phase != EditorPhase.Editing) return
        val draft = transform(current.draft)
        persist(draft)
        state.value = current.copy(draft = draft, errors = draft.fieldErrors())
    }

    fun onReminderSwitch(enabled: Boolean, delivery: NotificationDelivery) {
        update { it.copy(reminder = it.reminder.copy(enabled = enabled)) }
        if (enabled && shouldAsk(delivery)) ask(saveAfter = false)
    }

    fun onSaveClick(delivery: NotificationDelivery) {
        val current = state.value ?: return
        if (current.phase != EditorPhase.Editing) return
        if (current.errors.isNotEmpty()) {
            setShowErrors(true)
            state.value = current.copy(showErrors = true)
            return
        }
        if (current.draft.reminder.enabled && shouldAsk(delivery)) {
            ask(saveAfter = true)
        } else {
            saveNow()
        }
    }

    /** 시트의 "나중에"(또는 아래로 내려 닫기). OS 대화상자를 띄우지 않았으므로 요청 이력은 기록하지 않습니다. */
    fun onPermissionLater() {
        finishAsking()
    }

    /** OS 권한 대화상자의 결과(허용·거부 모두). 요청 이력을 기록합니다. */
    fun onPermissionRequestResult() {
        viewModelScope.launch {
            runCatchingNonCancellation { settings.markNotificationPermissionRequested() }
        }
        finishAsking()
    }

    fun messageShown() {
        state.update { it?.copy(message = null) }
    }

    private fun shouldAsk(delivery: NotificationDelivery): Boolean =
        delivery == NotificationDelivery.NeedsPermission(canAsk = true) && !askedThisScreen

    private fun ask(saveAfter: Boolean) {
        // 저장 중에 시트를 띄우면 "나중에" → Editing으로 저장 버튼이 다시 켜져 중복 저장이 될 수 있습니다.
        if (state.value?.phase != EditorPhase.Editing) return
        askedThisScreen = true
        state.update { it?.copy(phase = EditorPhase.AskingPermission(saveAfter)) }
    }

    private fun finishAsking() {
        val phase = state.value?.phase as? EditorPhase.AskingPermission ?: return
        state.update { it?.copy(phase = EditorPhase.Editing) }
        if (phase.saveAfter) saveNow()
    }

    private fun saveNow() {
        val current = state.value ?: return
        if (current.phase != EditorPhase.Editing) return
        state.value = current.copy(phase = EditorPhase.Saving, message = null)
        viewModelScope.launch {
            val result = save(current.draft, generation)
            state.update { s ->
                s ?: return@update null
                when (result) {
                    is Outcome.Ok -> s.copy(phase = EditorPhase.Saved(result.value))

                    is Outcome.Err -> when (val error = result.error) {
                        is SaveError.Invalid -> {
                            setShowErrors(true)
                            s.copy(
                                phase = EditorPhase.Editing,
                                errors = error.fields,
                                showErrors = true
                            )
                        }

                        SaveError.LimitReached -> s.failed(EditorMessage.LIMIT_REACHED)

                        SaveError.StorageFailed -> s.failed(EditorMessage.STORAGE_FAILED)

                        SaveError.NotReady -> s.failed(EditorMessage.NOT_READY)

                        SaveError.Missing -> s.copy(phase = EditorPhase.Missing)
                    }
                }
            }
        }
    }

    private fun EditorUiState.failed(message: EditorMessage) =
        copy(phase = EditorPhase.Editing, message = message)

    private fun editing(draft: SubscriptionDraft, showErrors: Boolean) = EditorUiState(
        isNew = isNew,
        draft = draft,
        errors = draft.fieldErrors(),
        showErrors = showErrors,
        phase = EditorPhase.Editing,
        message = null
    )

    private fun persist(draft: SubscriptionDraft) {
        handle[KEY_DRAFT] = DraftSaver.encode(draft)
    }

    private fun setShowErrors(value: Boolean) {
        handle[KEY_SHOW_ERRORS] = value
    }

    @AssistedFactory
    interface Factory {
        fun create(key: EditorNavKey): EditorViewModel
    }
}

/** 화면 입력 검증. 수정 중에도 사용 구간은 검증 결과에 영향이 없으므로 기존 구독 없이 검사합니다. */
private fun SubscriptionDraft.fieldErrors(): Set<FieldError> = when (val result = validate(null)) {
    is Outcome.Ok -> emptySet()
    is Outcome.Err -> result.error
}

internal const val KEY_DRAFT = "draft"
internal const val KEY_GENERATION = "generation"
private const val KEY_ASKED = "askedPermission"
private const val KEY_SHOW_ERRORS = "showErrors"
private const val STOP_TIMEOUT_MS = 5_000L
