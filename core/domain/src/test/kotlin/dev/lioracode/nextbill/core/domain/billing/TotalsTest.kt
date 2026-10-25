package dev.lioracode.nextbill.core.domain.billing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle.Months
import dev.lioracode.nextbill.core.model.BillingCycle.Weeks
import dev.lioracode.nextbill.core.model.BillingCycle.Years
import dev.lioracode.nextbill.core.model.CurrencyCode
import java.time.YearMonth
import org.junit.Test

class TotalsTest {
    @Test fun fixedCostExcludesCancelledAndSplitsCurrencies() {
        val subs = listOf(
            sub(id = 1, price = krw(10_000), cycle = Months(1)),
            sub(id = 2, price = krw(120_000), cycle = Years(1)),
            sub(id = 3, price = usd(999), cycle = Years(1)),
            sub(
                id = 4,
                price = krw(99_000),
                periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-03-01")))
            )
        )
        assertThat(monthlyFixedCost(subs)).containsExactly(krw(20_000), usd(83)).inOrder()
    }

    @Test fun dueTotalsIncludeCancelledBeforeCancelDate() {
        val cancelledAfterPayment = sub(
            id = 1,
            price = krw(10_000),
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-20")))
        )
        val cancelledBeforePayment = sub(
            id = 2,
            price = krw(5_000),
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-10")))
        )
        assertThat(
            dueTotals(listOf(cancelledAfterPayment, cancelledBeforePayment), YearMonth.of(2026, 11))
        ).containsExactly(krw(10_000))
    }

    @Test fun dueTotalsWeeklyFiveTimes() {
        val s = sub(price = krw(10_000), cycle = Weeks(1), anchor = "2026-11-02")
        assertThat(dueTotals(listOf(s), YearMonth.of(2026, 11))).containsExactly(krw(50_000))
    }

    @Test fun summarizePrimaryFirst() =
        assertThat(summarize(listOf(usd(299), krw(86_400)), CurrencyCode.KRW))
            .isEqualTo(AmountSummary(krw(86_400), listOf(usd(299))))

    @Test fun summarizeWithoutDefaultCurrency() =
        assertThat(summarize(listOf(usd(999)), CurrencyCode.KRW))
            .isEqualTo(AmountSummary(usd(999), emptyList()))

    @Test fun summarizeEmpty() = assertThat(summarize(emptyList(), CurrencyCode.KRW))
        .isEqualTo(AmountSummary(krw(0), emptyList()))

    @Test fun summarizeDropsZeroOthers() =
        assertThat(summarize(listOf(krw(100), usd(0)), CurrencyCode.KRW))
            .isEqualTo(AmountSummary(krw(100), emptyList()))
}
