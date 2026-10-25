package dev.lioracode.nextbill.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_periods",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subscriptionId")]
)
data class ActivePeriodEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val subscriptionId: Long,
    val startEpochDay: Long,
    val endEpochDay: Long?
)
