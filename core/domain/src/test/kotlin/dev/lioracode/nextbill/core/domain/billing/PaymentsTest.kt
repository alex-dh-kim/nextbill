package dev.lioracode.nextbill.core.domain.billing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.BillingCycle.Months
import dev.lioracode.nextbill.core.model.BillingCycle.Weeks
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import org.junit.Test

class PaymentsTest {
    private val november = d("2026-11-01")..d("2026-11-30")

    @Test fun weeklyHasFourOrFivePaymentsInMonth() {
        assertThat(
            paymentsIn(listOf(sub(anchor = "2026-11-02", cycle = Weeks(1))), november)
        ).hasSize(5)
        assertThat(
            paymentsIn(listOf(sub(anchor = "2026-11-03", cycle = Weeks(1))), november)
        ).hasSize(4)
    }

    @Test fun pastDatesInMonthAreIncluded() {
        // 오늘과 무관하게 달 전체의 결제일을 돌려줍니다. 11월 1일은 이미 지난 날짜여도 포함됩니다.
        val payments = paymentsIn(listOf(sub(anchor = "2026-09-01", cycle = Months(1))), november)
        assertThat(payments.map { it.date }).containsExactly(d("2026-11-01"))
        val early = paymentsIn(listOf(sub(anchor = "2026-01-02", cycle = Weeks(1))), november)
        assertThat(early.first().date).isEqualTo(d("2026-11-06"))
        assertThat(early.map { it.date }.filter { it < d("2026-11-15") }).isNotEmpty()
    }

    @Test fun cancelledKeepsPaymentsOnOrBeforeCancelDate() {
        val s = sub(anchor = "2026-10-05", cycle = Months(1))
        assertThat(paymentsIn(listOf(s.cancelledOn(d("2026-11-05"))), november)).hasSize(1)
        assertThat(paymentsIn(listOf(s.cancelledOn(d("2026-11-04"))), november)).isEmpty()
    }

    @Test fun pausedMonthHasNoPayment() {
        val s = sub(anchor = "2026-01-15").cancelledOn(d("2026-10-20")).restoredOn(d("2026-12-01"))
        assertThat(paymentsIn(listOf(s), november)).isEmpty()
    }

    @Test fun futureAnchorProducesNothingBefore() {
        assertThat(paymentsIn(listOf(sub(anchor = "2027-03-02")), november)).isEmpty()
    }

    @Test fun sortedByDateThenName() {
        val a = sub(id = 1, name = "Zeta", anchor = "2026-01-10")
        val b = sub(id = 2, name = "Alpha", anchor = "2026-01-10")
        val c = sub(id = 3, name = "Mid", anchor = "2026-01-05")
        val result = paymentsIn(listOf(a, b, c), november)
        assertThat(result.map { it.date to it.subscription.name }).containsExactly(
            d("2026-11-05") to "Mid",
            d("2026-11-10") to "Alpha",
            d("2026-11-10") to "Zeta"
        ).inOrder()
    }
}
