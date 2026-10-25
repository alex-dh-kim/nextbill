package dev.lioracode.nextbill.core.domain.reminder

import java.time.Instant

/**
 * OS 알람을 걸 시각: 기록 중 가장 이른 triggerAt. 이미 지났어도 now로 바꾸지 않습니다
 * (다시 걸지 판단할 때 비교하는 논리 시각, 설계 문서 ADR-4). 기록이 없으면 null.
 */
fun earliestTrigger(scheduled: Collection<PlannedReminder>): Instant? =
    scheduled.minOfOrNull { it.triggerAt }

/** 처리할 기록: triggerAt <= [now], triggerAt → 구독 id 순. */
fun dueReminders(scheduled: Collection<PlannedReminder>, now: Instant): List<PlannedReminder> =
    scheduled
        .filter { it.triggerAt <= now }
        .sortedWith(compareBy<PlannedReminder>({ it.triggerAt }, { it.subscriptionId.value }))
