package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.domain.billing.isBeforeFirstBilling
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import javax.inject.Inject

/** 오늘 날짜로 해지합니다. 없거나 이미 해지됐으면 MISSING, 첫 결제일 전이면 NOT_STARTED. */
class CancelSubscriptionUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val coordinator: DataCoordinator,
    private val refresher: DerivedStateRefresher,
    private val dateSource: CurrentDateSource
) {
    suspend operator fun invoke(id: SubscriptionId, generation: Long): Outcome<ActionError, Unit> =
        coordinator.writeAction(generation, refresher) {
            val today = dateSource.today.value
            val s = repo.get(id)
            when {
                s == null || s.status != SubscriptionStatus.ACTIVE -> Outcome.Err(
                    ActionError.MISSING
                )

                s.isBeforeFirstBilling(today) -> Outcome.Err(ActionError.NOT_STARTED)

                else -> {
                    repo.upsert(s.cancelledOn(today))
                    Outcome.Ok(Unit)
                }
            }
        }
}
