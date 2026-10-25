package dev.lioracode.nextbill.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.billing.billingDatesFrom
import dev.lioracode.nextbill.core.domain.billing.isBeforeFirstBilling
import dev.lioracode.nextbill.core.domain.billing.monthlyEquivalent
import dev.lioracode.nextbill.core.domain.billing.yearlyEquivalent
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.domain.usecase.ActionError
import dev.lioracode.nextbill.core.domain.usecase.CancelSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DeleteSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.RestoreSubscriptionUseCase
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 구독 상세(F3·F4). 구독과 오늘 날짜에서 화면 상태를 만들고, 해지·복원·삭제는 UseCase에 맡깁니다.
 * 화면 이동은 하지 않습니다. 끝나면 [DetailUiState.Closed]를 내보내고 화면이 닫습니다.
 *
 * [DetailUiState.Closed]는 끝 상태입니다: 한 번 내보낸 뒤에는 upstream을 끊어 이후 방출을 무시하고,
 * 구독자가 다시 붙어도 [Local.closed]가 남아 있어 Closed로 돌아갑니다.
 */
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted key: DetailNavKey,
    private val handle: SavedStateHandle,
    private val repo: SubscriptionRepository,
    private val dateSource: CurrentDateSource,
    settings: SettingsRepository,
    private val cancelSubscription: CancelSubscriptionUseCase,
    private val restoreSubscription: RestoreSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    coordinator: DataCoordinator
) : ViewModel() {
    private val id = SubscriptionId(key.subscriptionId)

    /**
     * 화면을 처음 열 때의 데이터 세대. 영속 값이라 복원해도 비교 기준이 같으므로 덮어쓰지 않습니다
     * (그 사이 가져오기가 있었다면 해지·삭제가 MISSING이 되어 화면이 닫혀야 함).
     */
    private val generation: Long =
        handle.get<Long>(KEY_GENERATION) ?: coordinator.generation.value.also {
            handle[KEY_GENERATION] = it
        }

    private data class Local(
        val busy: Boolean = false,
        val message: DetailMessage? = null,
        /** 이 화면의 해지가 진행 중. 저장소가 먼저 CANCELLED를 내보내도 읽기 전용 화면을 깜빡이지 않습니다. */
        val cancelling: Boolean = false,
        val closed: Boolean = false
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<DetailUiState> =
        combine(repo.observe(id), dateSource.today, local) { subscription, today, l ->
            when {
                l.closed || subscription == null -> DetailUiState.Closed

                l.cancelling && subscription.status == SubscriptionStatus.CANCELLED ->
                    DetailUiState.Closed

                else -> content(subscription, today, l)
            }
        }
            .transformWhile { state ->
                emit(state)
                state !is DetailUiState.Closed
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                DetailUiState.Loading
            )

    /** OS 권한 대화상자를 띄운 적이 있는지(알림 수신 가능 여부 계산용). */
    val permissionRequestedBefore: StateFlow<Boolean> = settings.notificationPermissionRequested
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun cancel() = act(closesOnOk = true, cancelling = true) { cancelSubscription(id, generation) }

    fun restore() = act(closesOnOk = false) { restoreSubscription(id, generation) }

    fun delete() = act(closesOnOk = true) { deleteSubscription(id, generation) }

    fun messageShown() {
        local.update { it.copy(message = null) }
    }

    private fun act(
        closesOnOk: Boolean,
        cancelling: Boolean = false,
        block: suspend () -> Outcome<ActionError, Unit>
    ) {
        var accepted = false
        local.update {
            accepted = !it.busy && !it.closed
            if (accepted) {
                it.copy(busy = true, message = null, cancelling = cancelling)
            } else {
                it
            }
        }
        if (!accepted) return
        viewModelScope.launch {
            val result = block()
            local.update { l ->
                when (result) {
                    is Outcome.Ok ->
                        if (closesOnOk) {
                            l.copy(busy = false, cancelling = false, closed = true)
                        } else {
                            l.copy(busy = false, cancelling = false)
                        }

                    is Outcome.Err -> when (result.error) {
                        ActionError.MISSING -> l.copy(
                            busy = false,
                            cancelling = false,
                            closed = true
                        )

                        ActionError.PERIOD_LIMIT -> l.failed(DetailMessage.PERIOD_LIMIT)

                        ActionError.NOT_READY -> l.failed(DetailMessage.NOT_READY)

                        // 이 화면에서는 일어나지 않습니다(첫 결제 전에는 해지 버튼이 없음). 방어적으로 일반 실패로 보입니다.
                        ActionError.NOT_STARTED, ActionError.STORAGE_FAILED ->
                            l.failed(DetailMessage.STORAGE_FAILED)
                    }
                }
            }
        }
    }

    private fun Local.failed(message: DetailMessage) =
        copy(busy = false, cancelling = false, message = message)

    private fun content(subscription: Subscription, today: LocalDate, l: Local): DetailUiState {
        val cancelledAt = subscription.cancelledAt
        val mode = when {
            subscription.isBeforeFirstBilling(today) -> DetailMode.BeforeFirstBilling
            cancelledAt != null -> DetailMode.Cancelled(cancelledAt)
            else -> DetailMode.Active
        }
        val upcoming = if (mode is DetailMode.Cancelled) {
            emptyList()
        } else {
            subscription.billingDatesFrom(today).take(UPCOMING_COUNT)
                .map { it to dueLabel(it, today) }
                .toList()
        }
        return DetailUiState.Content(
            subscription = subscription,
            mode = mode,
            upcoming = upcoming,
            monthly = subscription.monthlyEquivalent(),
            yearly = subscription.yearlyEquivalent(),
            busy = l.busy,
            message = l.message
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(key: DetailNavKey): DetailViewModel
    }
}

private const val KEY_GENERATION = "generation"
private const val UPCOMING_COUNT = 3
private const val STOP_TIMEOUT_MS = 5_000L
