package dev.lioracode.nextbill.core.domain.validation

import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Outcome.Err
import dev.lioracode.nextbill.core.model.Outcome.Ok
import java.math.BigDecimal
import java.util.Currency

// 쉼표는 세 자리 묶음 자리에만, 소수점은 최대 1개. 문법을 먼저 검사한 뒤에만 BigDecimal을 씁니다.
private val AMOUNT_SYNTAX = Regex("""(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?""")

internal fun fractionDigits(currency: CurrencyCode): Int {
    require(currency in CurrencyCode.SUPPORTED) { "Unsupported currency: ${currency.value}" }
    return Currency.getInstance(currency.value).defaultFractionDigits
}

/** 입력 문자열을 최소 단위 금액으로 바꿉니다. 0은 [FieldError.AMOUNT_BLANK]입니다. */
fun parseAmount(text: String, currency: CurrencyCode): Outcome<FieldError, Long> {
    val fractionDigits = fractionDigits(currency)
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Err(FieldError.AMOUNT_BLANK)
    if (!AMOUNT_SYNTAX.matches(trimmed)) return Err(FieldError.AMOUNT_INVALID)
    val number = BigDecimal(trimmed.replace(",", ""))
    if (number.scale() > fractionDigits) return Err(FieldError.AMOUNT_INVALID)
    val minor = number.movePointRight(fractionDigits).toBigIntegerExact()
    return when {
        minor.signum() == 0 -> Err(FieldError.AMOUNT_BLANK)
        minor > MAX_MINOR.toBigInteger() -> Err(FieldError.AMOUNT_TOO_LARGE)
        else -> Ok(minor.toLong())
    }
}
