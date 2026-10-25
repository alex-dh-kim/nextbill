package dev.lioracode.nextbill.core.domain.billing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.BillingCycle.Days
import dev.lioracode.nextbill.core.model.BillingCycle.Months
import dev.lioracode.nextbill.core.model.BillingCycle.Weeks
import dev.lioracode.nextbill.core.model.BillingCycle.Years
import org.junit.Test

class BillingDatesTest {
    @Test fun monthlyFromJan31ClampsWithoutDrift() =
        assertThat(billingDates(d("2026-01-31"), Months(1)).take(4).toList())
            .containsExactly(
                d("2026-01-31"),
                d("2026-02-28"),
                d("2026-03-31"),
                d("2026-04-30")
            ).inOrder()

    @Test fun leapYearFeb29() =
        assertThat(billingDates(d("2028-01-31"), Months(1)).elementAt(1)).isEqualTo(d("2028-02-29"))

    @Test fun yearlyFromFeb29() = assertThat(
        billingDates(d("2028-02-29"), Years(1)).take(5).toList()
    )
        .containsExactly(
            d("2028-02-29"),
            d("2029-02-28"),
            d("2030-02-28"),
            d("2031-02-28"),
            d("2032-02-29")
        ).inOrder()

    @Test fun everyTwoWeeks() = assertThat(billingDates(d("2026-11-02"), Weeks(2)).take(3).toList())
        .containsExactly(d("2026-11-02"), d("2026-11-16"), d("2026-11-30")).inOrder()

    @Test fun everyThreeMonths() = assertThat(
        billingDates(d("2026-01-31"), Months(3)).take(3).toList()
    )
        .containsExactly(d("2026-01-31"), d("2026-04-30"), d("2026-07-31")).inOrder()

    @Test fun nthBillingDateAddsOnceFromAnchor() {
        assertThat(nthBillingDate(d("2026-01-31"), Months(1), 0)).isEqualTo(d("2026-01-31"))
        assertThat(nthBillingDate(d("2026-01-31"), Months(1), 2)).isEqualTo(d("2026-03-31"))
        assertThat(nthBillingDate(d("2026-01-01"), Days(10), 3)).isEqualTo(d("2026-01-31"))
    }

    @Test fun nextBillingDateIsTodayWhenDueToday() = assertThat(
        sub(anchor = "2026-01-15", cycle = Months(1)).nextBillingDate(d("2026-11-15"))
    ).isEqualTo(d("2026-11-15"))

    @Test fun nextBillingDateAfterToday() = assertThat(
        sub(anchor = "2026-01-15", cycle = Months(1)).nextBillingDate(d("2026-11-16"))
    ).isEqualTo(d("2026-12-15"))

    @Test fun futureAnchorIsNextDate() = assertThat(
        sub(anchor = "2027-03-02", cycle = Years(1)).nextBillingDate(d("2026-11-01"))
    ).isEqualTo(d("2027-03-02"))

    @Test fun cancelledHasNoNextDate() =
        assertThat(sub().cancelledOn(d("2026-11-10")).nextBillingDate(d("2026-11-01"))).isNull()

    @Test fun farFromAnchor() {
        assertThat(
            sub(anchor = "1990-01-01", cycle = Days(1)).nextBillingDate(d("2026-11-15"))
        ).isEqualTo(d("2026-11-15"))
        assertThat(
            sub(anchor = "1990-01-31", cycle = Months(1)).nextBillingDate(d("2026-11-15"))
        ).isEqualTo(d("2026-11-30"))
    }

    @Test fun billingDatesFromSkipsEarlierDatesAndMatchesFullSequence() {
        val s = sub(anchor = "2000-01-31", cycle = Months(7))
        val from = d("2026-03-01")
        val expected = billingDates(s.anchorDate, s.cycle).dropWhile { it < from }.take(5).toList()
        assertThat(
            s.billingDatesFrom(from).take(5).toList()
        ).containsExactlyElementsIn(expected).inOrder()
    }

    @Test fun billingDatesFromStopsAtClosedLastPeriodEnd() {
        val s = sub(anchor = "2026-01-15").cancelledOn(d("2026-03-20"))
        assertThat(s.billingDatesFrom(d("2026-01-01")).toList())
            .containsExactly(d("2026-01-15"), d("2026-02-15"), d("2026-03-15")).inOrder()
    }

    @Test fun isBillingDateRequiresScheduleAndPeriod() {
        val s = sub(anchor = "2026-01-15")
        assertThat(s.isBillingDate(d("2026-05-15"))).isTrue()
        assertThat(s.isBillingDate(d("2026-05-16"))).isFalse()
        assertThat(s.isBillingDate(d("2025-12-15"))).isFalse()
    }
}
