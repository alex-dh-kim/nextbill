package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

fun krw(minor: Long) = Money(minor, CurrencyCode.KRW)

fun usd(minor: Long) = Money(minor, CurrencyCode.USD)

fun d(iso: String): LocalDate = LocalDate.parse(iso)

val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

/** 오프셋을 포함한 ISO 시각. 예: `at("2026-11-14T09:00+09:00")`. */
fun at(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()

/** 테스트용 구독. 기본값: 매월 15일, 기준일 2026-01-15. */
fun sub(
    id: Long = 1,
    name: String = "Netflix",
    anchor: String = "2026-01-15",
    cycle: BillingCycle = BillingCycle.Months(1),
    price: Money = Money(10_000, CurrencyCode.KRW),
    periods: List<ActivePeriod>? = null,
    reminder: Reminder = Reminder.DEFAULT
) = Subscription(
    id = SubscriptionId(id),
    name = name,
    price = price,
    cycle = cycle,
    anchorDate = d(anchor),
    category = Category.VIDEO,
    colorKey = ColorKey.RED,
    memo = "",
    reminder = reminder,
    periods = periods ?: newPeriods(d(anchor))
)
