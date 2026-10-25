package dev.lioracode.nextbill.core.database

import androidx.room.Embedded
import androidx.room.Relation

data class SubscriptionWithPeriods(
    @Embedded val subscription: SubscriptionEntity,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val periods: List<ActivePeriodEntity>
)
