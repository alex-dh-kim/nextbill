// 파일 이름은 reconcile 함수 기준입니다(계획서 Task 5). ReminderRecordAction은 그 결과 타입입니다.
@file:Suppress("ktlint:standard:filename")

package dev.lioracode.nextbill.core.domain.reminder

import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 예약 기록(DB)에 적용할 변경. */
sealed interface ReminderRecordAction {
    data class Put(val reminder: PlannedReminder) : ReminderRecordAction

    data class Remove(val id: SubscriptionId) : ReminderRecordAction
}

/**
 * 예약 기록을 지금 구독 상태에 맞춥니다(설계 문서 ADR-4). DB 기록만 다루고 OS 알람은 다루지 않습니다.
 * 시각이 지났지만 아직 유효한 기록은 알림 처리가 소비하도록 그대로 둡니다.
 * 결과 순서: [subscriptions] 순서, 그다음 구독이 없는 기록의 Remove(id 순).
 */
fun reconcile(
    subscriptions: List<Subscription>,
    scheduled: Map<SubscriptionId, PlannedReminder>,
    now: Instant,
    zone: ZoneId
): List<ReminderRecordAction> {
    val today = LocalDate.ofInstant(now, zone)
    val forSubscriptions = subscriptions.mapNotNull { sub ->
        val existing = scheduled[sub.id]
        val desired = sub.nextReminder(now, zone)
        if (existing != null && sub.isStillValid(existing, today, zone)) {
            when {
                existing.triggerAt <= now -> null
                existing == desired -> null
                desired != null -> ReminderRecordAction.Put(desired)
                else -> ReminderRecordAction.Remove(sub.id)
            }
        } else {
            when {
                desired != null -> ReminderRecordAction.Put(desired)
                existing != null -> ReminderRecordAction.Remove(sub.id)
                else -> null
            }
        }
    }
    val known = subscriptions.mapTo(HashSet()) { it.id }
    val orphans = scheduled.keys
        .filterNot { it in known }
        .sortedBy { it.value }
        .map { ReminderRecordAction.Remove(it) }
    return forSubscriptions + orphans
}
