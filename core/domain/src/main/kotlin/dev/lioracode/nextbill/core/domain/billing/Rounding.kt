package dev.lioracode.nextbill.core.domain.billing

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** 금액 반올림의 유일한 지점입니다. 정수 나눗셈을 HALF_EVEN으로 반올림합니다. */
internal fun divideHalfEven(numerator: BigInteger, denominator: BigInteger): Long =
    BigDecimal(numerator)
        .divide(BigDecimal(denominator), 0, RoundingMode.HALF_EVEN)
        .longValueExact()
