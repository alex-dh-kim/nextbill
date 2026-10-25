package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import javax.inject.Inject

/** 구독을 지웁니다. 알림 기록 정리는 갱신의 reconcile이 맡습니다. 없으면 MISSING. */
class DeleteSubscriptionUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val coordinator: DataCoordinator,
    private val refresher: DerivedStateRefresher
) {
    suspend operator fun invoke(id: SubscriptionId, generation: Long): Outcome<ActionError, Unit> =
        coordinator.writeAction(generation, refresher) {
            if (repo.get(id) == null) {
                Outcome.Err(ActionError.MISSING)
            } else {
                repo.delete(id)
                Outcome.Ok(Unit)
            }
        }
}
