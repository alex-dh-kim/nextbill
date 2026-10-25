package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** [anchor] + [cycle] * [n]. 누적하지 않고 기준일에서 한 번에 더합니다(월말 보정은 java.time 규칙). */
fun nthBillingDate(anchor: LocalDate, cycle: BillingCycle, n: Long): LocalDate {
    require(n >= 0) { "n must be >= 0" }
    val amount = cycle.interval.toLong() * n
    return when (cycle) {
        is BillingCycle.Days -> anchor.plusDays(amount)
        is BillingCycle.Weeks -> anchor.plusWeeks(amount)
        is BillingCycle.Months -> anchor.plusMonths(amount)
        is BillingCycle.Years -> anchor.plusYears(amount)
    }
}

/** 기준일부터의 모든 결제일(무한, 지연 생성). */
fun billingDates(anchor: LocalDate, cycle: BillingCycle): Sequence<LocalDate> =
    scheduleFrom(anchor, cycle, 0L)

/** [from] 이상, 사용 구간 안의 결제일. 마지막 구간이 닫혀 있으면 그 end에서 끝납니다. */
fun Subscription.billingDatesFrom(from: LocalDate): Sequence<LocalDate> {
    val firstN = approximateIndexOf(anchorDate, cycle, from)
    val lastEnd = periods.last().end
    val dates = scheduleFrom(anchorDate, cycle, firstN).dropWhile { it < from }
    val bounded = if (lastEnd == null) dates else dates.takeWhile { it <= lastEnd }
    return bounded.filter { date ->
        periods.any {
            date >= it.start &&
                (it.end == null || date <= it.end)
        }
    }
}

/** 일정에 있고 사용 구간 안이면 true. */
fun Subscription.isBillingDate(date: LocalDate): Boolean {
    val inPeriod = periods.any { date in it }
    if (!inPeriod) return false
    return scheduleFrom(anchorDate, cycle, approximateIndexOf(anchorDate, cycle, date))
        .dropWhile { it < date }
        .first() == date
}

/** ACTIVE 구독의 [today] 이후(당일 포함) 첫 결제일. 해지된 구독은 null. */
fun Subscription.nextBillingDate(today: LocalDate): LocalDate? {
    if (status != SubscriptionStatus.ACTIVE) return null
    return billingDatesFrom(today).firstOrNull()
}

private fun scheduleFrom(
    anchor: LocalDate,
    cycle: BillingCycle,
    firstN: Long
): Sequence<LocalDate> =
    generateSequence(firstN) { it + 1 }.map { nthBillingDate(anchor, cycle, it) }

/**
 * [target] 이전(또는 같은) 날짜에 해당하는 n의 근사값보다 2단계 앞. 월말 보정 때문에 근사가 1 어긋날 수 있어
 * 여유를 둡니다. 항상 nth(result) <= target 이거나 0입니다.
 */
private fun approximateIndexOf(anchor: LocalDate, cycle: BillingCycle, target: LocalDate): Long {
    if (target <= anchor) return 0L
    val unit = when (cycle) {
        is BillingCycle.Days -> ChronoUnit.DAYS.between(anchor, target)
        is BillingCycle.Weeks -> ChronoUnit.DAYS.between(anchor, target) / 7
        is BillingCycle.Months -> ChronoUnit.MONTHS.between(anchor, target)
        is BillingCycle.Years -> ChronoUnit.YEARS.between(anchor, target)
    }
    return maxOf(0L, unit / cycle.interval - 2)
}
