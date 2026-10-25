package dev.lioracode.nextbill.feature.calendar.impl

import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.usd
import java.time.YearMonth

internal fun novState(selected: String, withPayments: Boolean = true) = CalendarUiState(
    month = YearMonth.of(2026, 11),
    today = d("2026-11-15"),
    selected = d(selected),
    dots = mapOf(
        d("2026-11-15") to listOf(ColorKey.RED, ColorKey.BLUE),
        d("2026-11-16") to listOf(ColorKey.BLUE),
        d("2026-11-20") to listOf(ColorKey.PINK),
        d("2026-11-24") to listOf(ColorKey.SLATE),
        d("2026-11-27") to listOf(ColorKey.GREEN),
        d("2026-11-30") to listOf(ColorKey.AMBER, ColorKey.RED, ColorKey.BLUE)
    ),
    paymentCounts = mapOf(
        d("2026-11-15") to 2,
        d("2026-11-16") to 1,
        d("2026-11-20") to 1,
        d("2026-11-24") to 1,
        d("2026-11-27") to 1,
        d("2026-11-30") to 3
    ),
    monthCount = 7,
    monthTotal = AmountSummary(krw(86_400), listOf(usd(1_998))),
    selectedPayments = if (withPayments) {
        listOf(
            CalendarPayment(
                SubscriptionId(1),
                "넷플릭스",
                ColorKey.RED,
                krw(17_000),
                BillingCycle.Months(1),
                d("2026-01-15")
            ),
            CalendarPayment(
                SubscriptionId(2),
                "통신비",
                ColorKey.BLUE,
                krw(55_000),
                BillingCycle.Months(1),
                d("2026-01-15")
            )
        )
    } else {
        emptyList()
    }
)
