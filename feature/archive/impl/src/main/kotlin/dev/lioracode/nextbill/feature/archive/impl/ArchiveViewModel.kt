package dev.lioracode.nextbill.feature.archive.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.usecase.ActionError
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DeleteSubscriptionUseCase
import dev.lioracode.nextbill.core.domain.usecase.RestoreSubscriptionUseCase
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 해지한 구독 목록(F6). 목록은 저장소의 해지 구독을 그대로 보여 주고,
 * 다시 사용·삭제는 화면을 처음 열 때의 데이터 세대로 UseCase에 맡깁니다.
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    repo: SubscriptionRepository,
    private val restoreSubscription: RestoreSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    coordinator: DataCoordinator
) : ViewModel() {
    /** 영속 값이라 복원해도 덮어쓰지 않습니다(그 사이 가져오기가 있었다면 MISSING). */
    private val generation: Long =
        handle.get<Long>(KEY_GENERATION) ?: coordinator.generation.value.also {
            handle[KEY_GENERATION] = it
        }

    private data class Local(val busy: Boolean = false, val message: ArchiveMessage? = null)

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<ArchiveUiState> =
        combine(repo.observeCancelled(), local) { items, l ->
            ArchiveUiState.Ready(items, l.message)
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                ArchiveUiState.Loading
            )

    fun restore(id: SubscriptionId) = act(successMessage = ArchiveMessage.RESTORED) {
        restoreSubscription(id, generation)
    }

    fun delete(id: SubscriptionId) = act(successMessage = null) {
        deleteSubscription(id, generation)
    }

    fun messageShown() {
        local.update { it.copy(message = null) }
    }

    private fun act(
        successMessage: ArchiveMessage?,
        block: suspend () -> Outcome<ActionError, Unit>
    ) {
        var accepted = false
        local.update {
            accepted = !it.busy
            if (accepted) it.copy(busy = true, message = null) else it
        }
        if (!accepted) return
        viewModelScope.launch {
            val result = block()
            local.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is Outcome.Ok -> successMessage

                        is Outcome.Err -> when (result.error) {
                            ActionError.MISSING -> ArchiveMessage.MISSING

                            ActionError.PERIOD_LIMIT -> ArchiveMessage.PERIOD_LIMIT

                            ActionError.NOT_READY -> ArchiveMessage.NOT_READY

                            // 해지한 구독에는 첫 결제 전 상태가 없어 오지 않습니다. 방어적으로 일반 실패.
                            ActionError.NOT_STARTED, ActionError.STORAGE_FAILED ->
                                ArchiveMessage.STORAGE_FAILED
                        }
                    }
                )
            }
        }
    }
}

private const val KEY_GENERATION = "generation"
private const val STOP_TIMEOUT_MS = 5_000L
