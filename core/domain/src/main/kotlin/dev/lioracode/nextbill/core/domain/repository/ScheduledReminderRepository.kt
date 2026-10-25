package dev.lioracode.nextbill.core.domain.repository

import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.model.SubscriptionId

interface ScheduledReminderRepository {
    suspend fun getAll(): Map<SubscriptionId, PlannedReminder>

    suspend fun put(r: PlannedReminder)

    suspend fun remove(id: SubscriptionId)

    /**
     * 한 트랜잭션으로 지웁니다. 행마다 id·결제일·trigger가 모두 같을 때만 지우고,
     * 지운 행 수를 돌려줍니다.
     */
    suspend fun removeIfMatches(rs: Collection<PlannedReminder>): Int
}
