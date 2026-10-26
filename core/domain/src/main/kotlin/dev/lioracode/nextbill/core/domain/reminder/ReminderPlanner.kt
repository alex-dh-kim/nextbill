package dev.lioracode.nextbill.core.domain.reminder

import dev.lioracode.nextbill.core.domain.billing.billingDatesFrom
import dev.lioracode.nextbill.core.domain.billing.isBillingDate
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * [billingDate] 결제분의 알림 시각: (결제일 − daysBefore)의 reminder.time, [zone] 기준.
 * 없는 시각(DST 시작)은 `ZonedDateTime.of` 규칙대로 뒤로 밀립니다.
 */
fun Subscription.reminderTrigger(billingDate: LocalDate, zone: ZoneId): Instant =
    ZonedDateTime.of(billingDate.minusDays(reminder.daysBefore.toLong()), reminder.time, zone)
        .toInstant()

/** 알림 시각이 [now]보다 뒤인 가장 이른 결제분. 알림이 꺼졌거나 해지된 구독은 null. */
fun Subscription.nextReminder(now: Instant, zone: ZoneId): PlannedReminder? {
    if (!reminder.enabled || status != SubscriptionStatus.ACTIVE) return null
    // 오늘보다 이른 결제일의 알림 시각은 항상 now 이전이므로 오늘부터 찾습니다.
    val today = now.atZone(zone).toLocalDate()
    return billingDatesFrom(today)
        .map { PlannedReminder(id, it, reminderTrigger(it, zone)) }
        .firstOrNull { it.triggerAt > now }
}

/** 기록 [r]이 지금 설정에서도 그대로 유효하면 true. 알림 처리 시점에도 이 검사로 게시 여부를 정합니다. */
fun Subscription.isStillValid(r: PlannedReminder, today: LocalDate, zone: ZoneId): Boolean =
    reminder.enabled &&
        status == SubscriptionStatus.ACTIVE &&
        isBillingDate(r.billingDate) &&
        r.billingDate >= today &&
        r.triggerAt == reminderTrigger(r.billingDate, zone)
