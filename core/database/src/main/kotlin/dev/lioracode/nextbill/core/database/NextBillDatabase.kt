package dev.lioracode.nextbill.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** 파일명은 `nextbill.db`입니다(ADR-8 백업 규칙 대상). */
@Database(
    entities = [
        SubscriptionEntity::class,
        ActivePeriodEntity::class,
        ScheduledReminderEntity::class,
        DatasetMetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NextBillDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun scheduledReminderDao(): ScheduledReminderDao

    companion object {
        const val FILE_NAME = "nextbill.db"
    }
}
