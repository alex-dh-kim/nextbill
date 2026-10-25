package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.billing.canRestoreOn
import dev.lioracode.nextbill.core.domain.billing.restoredOn
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import javax.inject.Inject

/** 오늘 날짜로 다시 사용합니다. 없거나 해지 상태가 아니면 MISSING, 구간 한도면 PERIOD_LIMIT. */
class RestoreSubscriptionUseCase @Inject constructor(
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
                s == null || s.status != SubscriptionStatus.CANCELLED -> Outcome.Err(
                    ActionError.MISSING
                )

                !s.canRestoreOn(today) -> Outcome.Err(ActionError.PERIOD_LIMIT)

                else -> {
                    repo.upsert(s.restoredOn(today))
                    Outcome.Ok(Unit)
                }
            }
        }
}
