package dev.lioracode.nextbill.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun formatsEachSupportedCurrency() {
        assertThat(Money(17_000, CurrencyCode.KRW).format()).isEqualTo("₩17,000")
        assertThat(Money(999, CurrencyCode.USD).format()).isEqualTo("$9.99")
        assertThat(Money(1_998, CurrencyCode.USD).format()).isEqualTo("$19.98")
        assertThat(Money(500, CurrencyCode.JPY).format()).isEqualTo("¥500")
        assertThat(Money(1_050, CurrencyCode.EUR).format()).isEqualTo("€10.50")
    }

    @Test
    fun groupsThousandsAndKeepsSignAndSmallAmounts() {
        assertThat(Money(123_456_789, CurrencyCode.USD).format()).isEqualTo("$1,234,567.89")
        assertThat(Money(5, CurrencyCode.USD).format()).isEqualTo("$0.05")
        assertThat(Money(-1_050, CurrencyCode.EUR).format()).isEqualTo("-€10.50")
        assertThat(Money(Long.MIN_VALUE, CurrencyCode.KRW).format()).startsWith("-₩")
    }
}
