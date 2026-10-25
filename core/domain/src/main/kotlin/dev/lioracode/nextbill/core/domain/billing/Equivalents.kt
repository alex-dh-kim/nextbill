package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Subscription
import java.math.BigInteger

private const val DAYS_PER_YEAR = 365L
private const val MONTHS_PER_YEAR = 12L
private const val DAYS_PER_WEEK = 7L

/**
 * 월 환산. 먼저 곱하고 마지막에 한 번만 반올림합니다(HALF_EVEN).
 * Days: p*365/(12N), Weeks: p*365/(7*12N), Months: p/N, Years: p/(12N).
 * 곱셈은 BigInteger로 하므로 `price * 365`가 Long을 넘어도 안전합니다.
 */
fun Subscription.monthlyEquivalent(): Money {
    val p = BigInteger.valueOf(price.minor)
    val n = BigInteger.valueOf(cycle.interval.toLong())
    val (num, den) = when (cycle) {
        is BillingCycle.Days -> p * big(DAYS_PER_YEAR) to big(MONTHS_PER_YEAR) * n

        is BillingCycle.Weeks ->
            p * big(DAYS_PER_YEAR) to big(DAYS_PER_WEEK * MONTHS_PER_YEAR) * n

        is BillingCycle.Months -> p to n

        is BillingCycle.Years -> p to big(MONTHS_PER_YEAR) * n
    }
    return Money(divideHalfEven(num, den), price.currency)
}

/**
 * 연 환산. Days: p*365/N, Weeks: p*365/(7N), Months: p*12/N, Years: p/N.
 * 결과가 Long을 넘으면 `longValueExact`가 예외를 던집니다(가격은 MAX_MINOR = 1e12로 제한,
 * Task 6 검증: 1e12 * 365 = 3.65e14).
 */
fun Subscription.yearlyEquivalent(): Money {
    val p = BigInteger.valueOf(price.minor)
    val n = BigInteger.valueOf(cycle.interval.toLong())
    val (num, den) = when (cycle) {
        is BillingCycle.Days -> p * big(DAYS_PER_YEAR) to n
        is BillingCycle.Weeks -> p * big(DAYS_PER_YEAR) to big(DAYS_PER_WEEK) * n
        is BillingCycle.Months -> p * big(MONTHS_PER_YEAR) to n
        is BillingCycle.Years -> p to n
    }
    return Money(divideHalfEven(num, den), price.currency)
}

private fun big(v: Long): BigInteger = BigInteger.valueOf(v)
