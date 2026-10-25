package dev.lioracode.nextbill.core.data.mapping

import dev.lioracode.nextbill.core.database.ActivePeriodEntity
import dev.lioracode.nextbill.core.database.SubscriptionEntity
import dev.lioracode.nextbill.core.database.SubscriptionWithPeriods
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate
import java.time.LocalTime

private const val DAYS = "DAYS"
private const val WEEKS = "WEEKS"
private const val MONTHS = "MONTHS"
private const val YEARS = "YEARS"
private const val MINUTES_PER_HOUR = 60

/**
 * DB 행을 도메인 모델로 바꿉니다. 알 수 없는 enum 문자열은 기본값(주기 MONTHS, 분류 OTHER, 색 BLUE)으로
 * 대체합니다. 그 밖의 불변식(기준일 범위, 구간 순서 등)을 어긴 행은 [Subscription] 생성에서 예외가 납니다.
 */
fun SubscriptionWithPeriods.toModel(): Subscription {
    val e = subscription
    return Subscription(
        id = SubscriptionId(e.id),
        name = e.name,
        price = Money(e.priceMinor, CurrencyCode(e.currency)),
        cycle = cycleOf(e.cycleUnit, e.cycleInterval),
        anchorDate = LocalDate.ofEpochDay(e.anchorEpochDay),
        category = Category.entries.firstOrNull { it.name == e.category } ?: Category.OTHER,
        colorKey = ColorKey.entries.firstOrNull { it.name == e.colorKey } ?: ColorKey.BLUE,
        memo = e.memo,
        reminder = Reminder(
            enabled = e.reminderEnabled,
            daysBefore = e.reminderDaysBefore,
            time = LocalTime.of(
                e.reminderMinuteOfDay / MINUTES_PER_HOUR,
                e.reminderMinuteOfDay % MINUTES_PER_HOUR
            )
        ),
        periods = periods.sortedBy { it.startEpochDay }.map {
            ActivePeriod(
                LocalDate.ofEpochDay(it.startEpochDay),
                it.endEpochDay?.let(LocalDate::ofEpochDay)
            )
        }
    )
}

fun Subscription.toEntities(): Pair<SubscriptionEntity, List<ActivePeriodEntity>> {
    val entity = SubscriptionEntity(
        id = id.value,
        name = name,
        priceMinor = price.minor,
        currency = price.currency.value,
        cycleUnit = unitName(cycle),
        cycleInterval = cycle.interval,
        anchorEpochDay = anchorDate.toEpochDay(),
        category = category.name,
        colorKey = colorKey.name,
        memo = memo,
        reminderEnabled = reminder.enabled,
        reminderDaysBefore = reminder.daysBefore,
        reminderMinuteOfDay = reminder.time.hour * MINUTES_PER_HOUR + reminder.time.minute
    )
    val rows = periods.map {
        ActivePeriodEntity(
            subscriptionId = id.value,
            startEpochDay = it.start.toEpochDay(),
            endEpochDay = it.end?.toEpochDay()
        )
    }
    return entity to rows
}

private fun cycleOf(unit: String, interval: Int): BillingCycle = when (unit) {
    DAYS -> BillingCycle.Days(interval)
    WEEKS -> BillingCycle.Weeks(interval)
    YEARS -> BillingCycle.Years(interval)
    else -> BillingCycle.Months(interval)
}

private fun unitName(cycle: BillingCycle): String = when (cycle) {
    is BillingCycle.Days -> DAYS
    is BillingCycle.Weeks -> WEEKS
    is BillingCycle.Months -> MONTHS
    is BillingCycle.Years -> YEARS
}
