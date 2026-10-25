package dev.lioracode.nextbill.core.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * 표시용 금액 문자열. 기호는 `Currency.getSymbol(Locale.US)`, 소수 자릿수는 `defaultFractionDigits`,
 * 천 단위는 ",", 음수는 기호 앞에 "-"입니다. 예: `₩17,000`, `$9.99`.
 */
fun Money.format(): String {
    val currency = Currency.getInstance(currency.value)
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    val amount = BigDecimal.valueOf(minor).movePointLeft(digits)
    val pattern = if (digits == 0) "#,##0" else "#,##0." + "0".repeat(digits)
    val number = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(amount.abs())
    val sign = if (minor < 0) "-" else ""
    return sign + currency.getSymbol(Locale.US) + number
}
