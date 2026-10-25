package dev.lioracode.nextbill.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class ScheduledReminderDao {
    @Query("SELECT * FROM scheduled_reminders")
    abstract suspend fun getAll(): List<ScheduledReminderEntity>

    @Upsert
    abstract suspend fun put(e: ScheduledReminderEntity)

    @Query("DELETE FROM scheduled_reminders WHERE subscriptionId = :id")
    abstract suspend fun remove(id: Long)

    /** 같은 구독의 더 새로운 행(다른 결제일·시각)은 건드리지 않습니다. */
    @Query(
        "DELETE FROM scheduled_reminders WHERE subscriptionId = :id " +
            "AND billingEpochDay = :billing AND triggerAtEpochMilli = :trigger"
    )
    abstract suspend fun removeIfMatches(id: Long, billing: Long, trigger: Long): Int

    @Transaction
    open suspend fun removeAllIfMatches(items: List<ScheduledReminderEntity>): Int = items.sumOf {
        removeIfMatches(it.subscriptionId, it.billingEpochDay, it.triggerAtEpochMilli)
    }
}
