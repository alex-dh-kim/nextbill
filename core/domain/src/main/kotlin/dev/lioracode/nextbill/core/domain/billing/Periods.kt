package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate

/** [date]가 이 구간 안(양 끝 포함)이면 true. */
internal operator fun ActivePeriod.contains(date: LocalDate): Boolean =
    date >= start && end.let { it == null || date <= it }

/** 새 구독의 사용 구간. */
fun newPeriods(anchor: LocalDate): List<ActivePeriod> = listOf(ActivePeriod(anchor, null))

/** 첫 결제일이 아직 오지 않은 구독(해지할 수 없고 삭제만 가능). */
fun Subscription.isBeforeFirstBilling(today: LocalDate): Boolean =
    periods.size == 1 && anchorDate > today

fun Subscription.canRestoreOn(date: LocalDate): Boolean {
    val lastEnd = periods.last().end ?: return false
    return date <= lastEnd.plusDays(1) || periods.size < MAX_PERIODS
}

/** [date]에 해지합니다. 마지막 구간이 [date]보다 늦게 시작하면(시스템 날짜를 되돌린 경우) 그 구간을 지웁니다. */
fun Subscription.cancelledOn(date: LocalDate): Subscription {
    require(status == SubscriptionStatus.ACTIVE) { "Only an active subscription can be cancelled" }
    require(!isBeforeFirstBilling(date)) { "Cannot cancel before the first billing date" }
    val last = periods.last()
    val updated = if (date <
        last.start
    ) {
        periods.dropLast(1)
    } else {
        periods.dropLast(1) + last.copy(end = date)
    }
    return copy(periods = updated)
}

/** [date]에 복원합니다. 같은 날·다음 날 복원은 마지막 구간을 다시 열고, 아니면 새 구간을 추가합니다. */
fun Subscription.restoredOn(date: LocalDate): Subscription {
    require(canRestoreOn(date)) { "Cannot restore on $date" }
    val last = periods.last()
    val updated = if (date <= last.end!!.plusDays(1)) {
        periods.dropLast(1) + last.copy(end = null)
    } else {
        periods + ActivePeriod(date, null)
    }
    return copy(periods = updated)
}

/** 기준일을 옮깁니다. 새 기준일보다 먼저 끝난 구간은 지우고, 첫 구간의 시작을 새 기준일로 맞춥니다. */
fun Subscription.withAnchor(newAnchor: LocalDate): Subscription {
    require(status == SubscriptionStatus.ACTIVE) {
        "Only an active subscription can change its anchor"
    }
    val kept = periods.filter { it.end.let { end -> end == null || end >= newAnchor } }
    return copy(
        anchorDate = newAnchor,
        periods = listOf(kept.first().copy(start = newAnchor)) + kept.drop(1)
    )
}
