package dev.lioracode.nextbill.core.domain.reminder

import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.Instant
import java.time.LocalDate

/** 구독 하나의 알림 예약 기록. 결제일 하나(occurrence)에 묶입니다(설계 문서 ADR-3·4). */
data class PlannedReminder(
    val subscriptionId: SubscriptionId,
    val billingDate: LocalDate,
    val triggerAt: Instant
)
