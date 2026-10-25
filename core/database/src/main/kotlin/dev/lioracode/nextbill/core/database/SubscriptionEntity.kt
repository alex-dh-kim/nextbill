package dev.lioracode.nextbill.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val priceMinor: Long,
    val currency: String,
    val cycleUnit: String,
    val cycleInterval: Int,
    val anchorEpochDay: Long,
    val category: String,
    val colorKey: String,
    val memo: String,
    val reminderEnabled: Boolean,
    val reminderDaysBefore: Int,
    val reminderMinuteOfDay: Int
)
