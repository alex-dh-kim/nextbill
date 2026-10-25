package dev.lioracode.nextbill.core.domain.validation

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.validation.FieldError.AMOUNT_BLANK
import dev.lioracode.nextbill.core.domain.validation.FieldError.AMOUNT_INVALID
import dev.lioracode.nextbill.core.domain.validation.FieldError.AMOUNT_TOO_LARGE
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.CurrencyCode.Companion.EUR
import dev.lioracode.nextbill.core.model.CurrencyCode.Companion.JPY
import dev.lioracode.nextbill.core.model.CurrencyCode.Companion.KRW
import dev.lioracode.nextbill.core.model.CurrencyCode.Companion.USD
import dev.lioracode.nextbill.core.model.Outcome.Err
import dev.lioracode.nextbill.core.model.Outcome.Ok
import org.junit.Assert.assertThrows
import org.junit.Test

class AmountParserTest {
    @Test fun parses() {
        assertThat(parseAmount("17000", KRW)).isEqualTo(Ok(17_000L))
        assertThat(parseAmount("17,000", KRW)).isEqualTo(Ok(17_000L))
        assertThat(parseAmount(" 1,234,567 ", KRW)).isEqualTo(Ok(1_234_567L))
        assertThat(parseAmount("9.99", USD)).isEqualTo(Ok(999L))
        assertThat(parseAmount("9.9", USD)).isEqualTo(Ok(990L))
        assertThat(parseAmount("1,000.50", USD)).isEqualTo(Ok(100_050L))
        assertThat(parseAmount("500", JPY)).isEqualTo(Ok(500L))
        assertThat(parseAmount("1000000000000", KRW)).isEqualTo(Ok(1_000_000_000_000L))
        assertThat(parseAmount("12,345.67", USD)).isEqualTo(Ok(1_234_567L))
    }

    @Test fun rejects() {
        assertThat(parseAmount("", KRW)).isEqualTo(Err(AMOUNT_BLANK))
        assertThat(parseAmount("   ", KRW)).isEqualTo(Err(AMOUNT_BLANK))
        assertThat(parseAmount("0", KRW)).isEqualTo(Err(AMOUNT_BLANK))
        assertThat(parseAmount("0.00", USD)).isEqualTo(Err(AMOUNT_BLANK))
        assertThat(parseAmount("1.5", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("9.999", USD)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("1,5", USD)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("10,00", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount(",100", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("1e3", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("-5", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("+5", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("1.2.3", USD)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("abc", KRW)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("5.", USD)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount(".5", USD)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("99999999999999999999", KRW)).isEqualTo(Err(AMOUNT_TOO_LARGE))
        assertThat(parseAmount("1000000000001", KRW)).isEqualTo(Err(AMOUNT_TOO_LARGE))
        assertThat(parseAmount("10000000000.01", USD)).isEqualTo(Err(AMOUNT_TOO_LARGE))
    }

    @Test fun fractionDigitsFollowCurrency() {
        assertThat(parseAmount("1.5", JPY)).isEqualTo(Err(AMOUNT_INVALID))
        assertThat(parseAmount("10.50", EUR)).isEqualTo(Ok(1050L))
    }

    @Test fun unsupportedCurrencyThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            parseAmount("1", CurrencyCode("GBP"))
        }
    }
}
