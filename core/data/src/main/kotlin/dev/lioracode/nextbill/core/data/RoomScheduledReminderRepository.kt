package dev.lioracode.nextbill.core.data

import dev.lioracode.nextbill.core.database.ScheduledReminderDao
import dev.lioracode.nextbill.core.database.ScheduledReminderEntity
import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.domain.repository.ScheduledReminderRepository
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RoomScheduledReminderRepository @Inject constructor(private val dao: ScheduledReminderDao) :
    ScheduledReminderRepository {
    override suspend fun getAll(): Map<SubscriptionId, PlannedReminder> =
        dao.getAll().map { it.toModel() }.associateBy { it.subscriptionId }

    override suspend fun put(r: PlannedReminder) = dao.put(r.toEntity())

    override suspend fun remove(id: SubscriptionId) = dao.remove(id.value)

    override suspend fun removeIfMatches(rs: Collection<PlannedReminder>): Int =
        dao.removeAllIfMatches(rs.map { it.toEntity() })

    private fun ScheduledReminderEntity.toModel() = PlannedReminder(
        SubscriptionId(subscriptionId),
        LocalDate.ofEpochDay(billingEpochDay),
        Instant.ofEpochMilli(triggerAtEpochMilli)
    )

    private fun PlannedReminder.toEntity() = ScheduledReminderEntity(
        subscriptionId.value,
        billingDate.toEpochDay(),
        triggerAt.toEpochMilli()
    )
}
