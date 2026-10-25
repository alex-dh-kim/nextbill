package dev.lioracode.nextbill.core.domain.billing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.BillingCycle.Days
import dev.lioracode.nextbill.core.model.BillingCycle.Months
import dev.lioracode.nextbill.core.model.BillingCycle.Weeks
import dev.lioracode.nextbill.core.model.BillingCycle.Years
import org.junit.Test

class EquivalentsTest {
    @Test fun yearlyToMonthly() =
        assertThat(sub(price = krw(120_000), cycle = Years(1)).monthlyEquivalent())
            .isEqualTo(krw(10_000))

    @Test fun weeklyToMonthly() =
        assertThat(sub(price = krw(10_000), cycle = Weeks(1)).monthlyEquivalent())
            .isEqualTo(krw(43_452)) // 3,650,000/84 = 43452.38

    @Test fun dailyToMonthly() = assertThat(
        sub(price = krw(1_000), cycle = Days(1)).monthlyEquivalent()
    )
        .isEqualTo(krw(30_417)) // 365000/12 = 30416.67

    @Test fun weeklyAndDailyAgree() =
        assertThat(sub(price = krw(7_000), cycle = Weeks(1)).monthlyEquivalent())
            .isEqualTo(sub(price = krw(1_000), cycle = Days(1)).monthlyEquivalent())

    @Test fun halfEvenRounding() {
        assertThat(sub(price = krw(30), cycle = Months(4)).monthlyEquivalent()).isEqualTo(krw(8))
        assertThat(sub(price = krw(50), cycle = Months(4)).monthlyEquivalent()).isEqualTo(krw(12))
    }

    @Test fun usdCents() = assertThat(sub(price = usd(999), cycle = Years(1)).monthlyEquivalent())
        .isEqualTo(usd(83)) // 83.25

    @Test fun netflixYearly() = assertThat(
        sub(price = krw(17_000), cycle = Months(1)).yearlyEquivalent()
    )
        .isEqualTo(krw(204_000))

    @Test fun hugeDailyAmountDoesNotOverflow() =
        assertThat(sub(price = krw(1_000_000_000_000), cycle = Days(1)).yearlyEquivalent())
            .isEqualTo(krw(365_000_000_000_000))
}
