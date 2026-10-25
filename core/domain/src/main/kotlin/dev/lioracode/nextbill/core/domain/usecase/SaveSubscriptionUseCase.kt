package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.domain.validation.validate
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import javax.inject.Inject

sealed interface SaveError {
    data class Invalid(val fields: Set<FieldError>) : SaveError

    data object LimitReached : SaveError

    /** 사라졌거나 해지된 구독, 또는 가져오기 전에 연 화면의 데이터. */
    data object Missing : SaveError

    data object NotReady : SaveError

    data object StorageFailed : SaveError
}

/**
 * 구독을 저장합니다. DB에 반영되면 성공이고, 파생 상태 갱신은 잠금 밖에서 따로 요청합니다(ADR-13).
 *
 * 허용한 위험(ADR-13): DB 저장 직후, 화면이 결과를 받기 전에 프로세스가 죽으면 복원된 입력으로 다시
 * 저장해 같은 구독이 2건이 될 수 있습니다. 막지 않으며, 발생 빈도는 측정하지 않았습니다.
 */
class SaveSubscriptionUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val coordinator: DataCoordinator,
    private val refresher: DerivedStateRefresher
) {
    suspend operator fun invoke(
        draft: SubscriptionDraft,
        generation: Long
    ): Outcome<SaveError, SubscriptionId> {
        val admission = coordinator.write(generation) {
            runCatchingNonCancellation { saveLocked(draft) }
        }
        val outcome = when (admission) {
            Admission.NotReady -> Outcome.Err(SaveError.NotReady)
            Admission.Stale -> Outcome.Err(SaveError.Missing)
            is Admission.Ran -> admission.value.getOrElse { Outcome.Err(SaveError.StorageFailed) }
        }
        if (outcome is Outcome.Ok) refresher.request()
        return outcome
    }

    private suspend fun saveLocked(draft: SubscriptionDraft): Outcome<SaveError, SubscriptionId> {
        val existing = if (draft.id.value != 0L) {
            // 사라진 구독을 되살리지 않고, 그사이 해지된 구독은 withAnchor 전에 거릅니다.
            val found = repo.get(draft.id)
            if (found == null || found.status != SubscriptionStatus.ACTIVE) {
                return Outcome.Err(SaveError.Missing)
            }
            found
        } else {
            null
        }
        val subscription = when (val validated = draft.validate(existing)) {
            is Outcome.Err -> return Outcome.Err(SaveError.Invalid(validated.error))
            is Outcome.Ok -> validated.value
        }
        if (existing == null && repo.count() >= MAX_SUBSCRIPTIONS) {
            return Outcome.Err(SaveError.LimitReached)
        }
        return Outcome.Ok(repo.upsert(subscription))
    }
}
