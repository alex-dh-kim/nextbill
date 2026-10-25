package dev.lioracode.nextbill.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 구독에 FK를 두지 않습니다. 삭제된 구독의 알람을 reconcile이 찾아 취소해야 하기 때문입니다. */
@Entity(tableName = "scheduled_reminders")
data class ScheduledReminderEntity(
    @PrimaryKey val subscriptionId: Long,
    val billingEpochDay: Long,
    val triggerAtEpochMilli: Long
)
