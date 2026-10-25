package dev.lioracode.nextbill.feature.archive.impl

import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub

/** 시안 04: 해지일 최신순 세 건. */
internal val archiveReady = ArchiveUiState.Ready(
    items = listOf(
        sub(2, "왓챠", "2026-01-12", price = Money(12_900, CurrencyCode.KRW))
            .cancelledOn(d("2026-05-20")),
        sub(1, "밀리의 서재", "2026-01-14", price = Money(9_900, CurrencyCode.KRW))
            .cancelledOn(d("2026-03-02")),
        sub(3, "쿠팡 와우 멤버십", "2026-01-28", price = Money(7_890, CurrencyCode.KRW))
            .cancelledOn(d("2026-03-02"))
    ),
    message = null
)
