package dev.lioracode.nextbill.core.model

/** 구독 ID. 새 구독은 `SubscriptionId(0)`입니다. */
@JvmInline
value class SubscriptionId(val value: Long)

/** ISO 4217 통화 코드. 지원 통화는 [SUPPORTED]뿐입니다. */
@JvmInline
value class CurrencyCode(val value: String) {
    companion object {
        val KRW = CurrencyCode("KRW")
        val USD = CurrencyCode("USD")
        val EUR = CurrencyCode("EUR")
        val JPY = CurrencyCode("JPY")
        val SUPPORTED = listOf(KRW, USD, EUR, JPY)
    }
}

/** 금액. [minor]는 통화의 최소 단위(원, 센트 등) 정수입니다. Double/Float로 계산하지 않습니다. */
data class Money(val minor: Long, val currency: CurrencyCode) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot add ${currency.value} and ${other.currency.value}"
        }
        return Money(Math.addExact(minor, other.minor), currency)
    }
}
