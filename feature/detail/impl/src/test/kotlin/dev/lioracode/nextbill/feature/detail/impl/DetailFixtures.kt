package dev.lioracode.nextbill.feature.detail.impl

import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub

/** 시안 03: 넷플릭스, 오늘(2026-11-15)이 결제일, 메모가 있는 활성 구독. */
internal val netflixActive = DetailUiState.Content(
    subscription = TestSubscriptions.netflix.copy(memo = "스탠다드 요금제 4K 지원"),
    mode = DetailMode.Active,
    upcoming = listOf(
        d("2026-11-15") to DueLabel.Today,
        d("2026-12-15") to DueLabel.InDays(30),
        d("2027-01-15") to DueLabel.InDays(61)
    ),
    monthly = krw(17_000),
    yearly = krw(204_000),
    busy = false,
    message = null
)

internal val netflixCancelled = netflixActive.copy(
    subscription = TestSubscriptions.netflix.copy(memo = "스탠다드 요금제 4K 지원")
        .cancelledOn(d("2026-03-02")),
    mode = DetailMode.Cancelled(d("2026-03-02")),
    upcoming = emptyList()
)

internal val beforeFirstBilling = netflixActive.copy(
    subscription = sub(1, "넷플릭스", "2026-12-01", price = krw(17_000)),
    mode = DetailMode.BeforeFirstBilling,
    upcoming = listOf(
        d("2026-12-01") to DueLabel.InDays(37),
        d("2027-01-01") to DueLabel.InDays(68),
        d("2027-02-01") to DueLabel.InDays(99)
    )
)
