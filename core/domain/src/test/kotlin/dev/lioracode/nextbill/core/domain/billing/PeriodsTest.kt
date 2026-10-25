package dev.lioracode.nextbill.core.domain.billing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class PeriodsTest {
    private val base = sub(anchor = "2026-01-15")

    @Test fun restoreAfterMonthsDoesNotBackfill() {
        val s = base.cancelledOn(d("2026-01-20")).restoredOn(d("2026-04-20"))
        assertThat(s.isBillingDate(d("2026-01-15"))).isTrue()
        assertThat(s.isBillingDate(d("2026-02-15"))).isFalse()
        assertThat(s.isBillingDate(d("2026-03-15"))).isFalse()
        assertThat(s.isBillingDate(d("2026-04-15"))).isFalse()
        assertThat(s.isBillingDate(d("2026-05-15"))).isTrue()
        assertThat(s.nextBillingDate(d("2026-04-20"))).isEqualTo(d("2026-05-15"))
    }

    @Test fun cancelAgainAndRestoreAgain() {
        val s = base.cancelledOn(d("2026-01-20")).restoredOn(d("2026-04-20"))
            .cancelledOn(d("2026-06-01")).restoredOn(d("2026-08-10"))
        assertThat(s.isBillingDate(d("2026-06-15"))).isFalse()
        assertThat(s.isBillingDate(d("2026-07-15"))).isFalse()
        assertThat(s.isBillingDate(d("2026-08-15"))).isTrue()
        assertThat(s.periods.size).isEqualTo(3)
    }

    @Test fun sameDayRestoreReopensLastPeriod() {
        val s = base.cancelledOn(d("2026-03-15")).restoredOn(d("2026-03-15"))
        assertThat(s.periods).containsExactly(ActivePeriod(d("2026-01-15"), null))
        assertThat(
            s.billingDatesFrom(d("2026-03-01")).take(1).toList()
        ).containsExactly(d("2026-03-15"))
        assertThat(
            s.billingDatesFrom(d("2026-03-01")).takeWhile {
                it <= d("2026-03-31")
            }.count()
        ).isEqualTo(1)
    }

    @Test fun nextDayRestoreReopensLastPeriod() {
        val s = base.cancelledOn(d("2026-03-15")).restoredOn(d("2026-03-16"))
        assertThat(s.periods.size).isEqualTo(1)
        assertThat(s.status).isEqualTo(SubscriptionStatus.ACTIVE)
    }

    @Test fun cancelOnBillingDateKeepsThatPayment() {
        val s = base.cancelledOn(d("2026-03-15"))
        assertThat(s.isBillingDate(d("2026-03-15"))).isTrue()
        assertThat(s.isBillingDate(d("2026-04-15"))).isFalse()
        assertThat(s.cancelledAt).isEqualTo(d("2026-03-15"))
    }

    @Test fun withAnchorMovesFirstPeriodStart() {
        val s = base.withAnchor(d("2026-02-10"))
        assertThat(s.periods.first().start).isEqualTo(d("2026-02-10"))
        assertThat(s.anchorDate).isEqualTo(d("2026-02-10"))
        assertThat(s.isBillingDate(d("2026-01-15"))).isFalse()
    }

    @Test fun futureAnchorIsBeforeFirstBilling() {
        val s = sub(anchor = "2026-12-01")
        assertThat(s.isBeforeFirstBilling(d("2026-10-24"))).isTrue()
        assertThat(s.isBeforeFirstBilling(d("2026-12-01"))).isFalse()
        assertThrows(IllegalArgumentException::class.java) { s.cancelledOn(d("2026-10-24")) }
    }

    @Test fun cancelBeforeRestoredPeriodStartsDropsIt() {
        val s = base.cancelledOn(
            d("2026-03-15")
        ).restoredOn(d("2026-05-01")).cancelledOn(d("2026-04-20"))
        assertThat(s.periods).containsExactly(ActivePeriod(d("2026-01-15"), d("2026-03-15")))
        assertThat(s.cancelledAt).isEqualTo(d("2026-03-15"))
    }

    @Test fun restoreLimit() {
        val start = d("2020-01-01")
        val periods = (0 until MAX_PERIODS).map { i ->
            val s = start.plusDays(i * 10L)
            ActivePeriod(s, s.plusDays(5))
        }
        val s = sub(anchor = "2020-01-01", periods = periods)
        val lastEnd = periods.last().end!!
        assertThat(s.canRestoreOn(lastEnd.plusDays(10))).isFalse()
        assertThat(s.canRestoreOn(lastEnd.plusDays(1))).isTrue()
        assertThat(s.canRestoreOn(lastEnd)).isTrue()
        assertThat(s.restoredOn(lastEnd.plusDays(1)).periods.size).isEqualTo(MAX_PERIODS)
        assertThrows(IllegalArgumentException::class.java) { s.restoredOn(lastEnd.plusDays(10)) }
    }

    @Test fun withAnchorDropsPeriodsBeforeNewAnchor() {
        val s = sub(
            anchor = "2026-01-15",
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-01-20")),
                ActivePeriod(d("2026-04-20"), null)
            )
        )
        assertThat(
            s.withAnchor(d("2026-05-01")).periods
        ).containsExactly(ActivePeriod(d("2026-05-01"), null))
    }

    @Test fun invalidTransitionsThrow() {
        assertThrows(IllegalArgumentException::class.java) { base.restoredOn(d("2026-03-01")) }
        assertThrows(IllegalArgumentException::class.java) {
            base.cancelledOn(d("2026-03-15")).cancelledOn(d("2026-03-16"))
        }
    }

    @Test fun subscriptionRejectsBrokenPeriods() {
        fun build(vararg p: ActivePeriod, anchor: String = "2026-01-15"): Subscription =
            sub(anchor = anchor, periods = p.toList())

        fun p(s: String, e: String?) = ActivePeriod(d(s), e?.let(LocalDate::parse))
        // 겹침
        assertThrows(IllegalArgumentException::class.java) {
            build(p("2026-01-15", "2026-03-01"), p("2026-02-01", null))
        }
        // 시간 역순
        assertThrows(IllegalArgumentException::class.java) {
            build(p("2026-04-01", "2026-04-10"), p("2026-01-15", null), anchor = "2026-04-01")
        }
        // 중간 열린 구간
        assertThrows(IllegalArgumentException::class.java) {
            build(p("2026-01-15", null), p("2026-05-01", null))
        }
        // first.start != anchor
        assertThrows(IllegalArgumentException::class.java) { build(p("2026-02-01", null)) }
        // 빈 목록
        assertThrows(IllegalArgumentException::class.java) { build() }
        // end < start
        assertThrows(IllegalArgumentException::class.java) {
            ActivePeriod(d("2026-02-01"), d("2026-01-01"))
        }
    }
}
