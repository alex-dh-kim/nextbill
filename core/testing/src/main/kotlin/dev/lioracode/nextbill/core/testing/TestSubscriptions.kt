package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.billing.newPeriods
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
    reminder: Reminder = Reminder.DEFAULT,
    category: Category = Category.VIDEO,
    colorKey: ColorKey = ColorKey.RED
) = Subscription(
    id = SubscriptionId(id),
    name = name,
    price = price,
    cycle = cycle,
    anchorDate = d(anchor),
    category = category,
    colorKey = colorKey,
    memo = "",
    reminder = reminder,
    periods = periods ?: newPeriods(d(anchor))
)

/** 시안 00(홈)의 예시 구독 6건. id는 1~6. */
object TestSubscriptions {
    val netflix = sub(1, "넷플릭스", "2026-01-15", price = krw(17_000))

    val phoneBill = sub(
        2,
        "통신비",
        "2026-01-16",
        price = krw(55_000),
        category = Category.TELECOM,
        colorKey = ColorKey.BLUE
    )

    val youtube = sub(
        3,
        "유튜브 프리미엄",
        "2026-01-20",
        price = krw(14_900),
        category = Category.VIDEO,
        colorKey = ColorKey.PINK
    )

    val icloud = sub(
        4,
        "iCloud+",
        "2026-01-24",
        price = usd(299),
        category = Category.CLOUD,
        colorKey = ColorKey.SLATE
    )

    val gym = sub(
        5,
        "헬스장",
        "2026-01-27",
        price = krw(60_000),
        category = Category.LIVING,
        colorKey = ColorKey.GREEN
    )

    val carInsurance = sub(
        6,
        "자동차보험",
        "2025-03-02",
        cycle = BillingCycle.Years(1),
        price = krw(480_000),
        category = Category.INSURANCE,
        colorKey = ColorKey.AMBER
    )

    val all = listOf(netflix, phoneBill, youtube, icloud, gym, carInsurance)
}
