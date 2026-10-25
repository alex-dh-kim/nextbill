package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.YearMonth

/**
 * 사용 중인 구독의 월 고정비를 통화별로 합산합니다(통화 코드 순). 구독별로 먼저 반올림한 뒤 더합니다.
 *
 * 오버플로 여지: 합산은 `Money.plus`(addExact)입니다. 구독은 MAX_SUBSCRIPTIONS = 1,000건,
 * 가격은 MAX_MINOR = 1e12(Task 6 검증)이므로 월 환산 합은 1,000 * 1e12 = 1e15 이하입니다.
 */
fun monthlyFixedCost(subscriptions: List<Subscription>): List<Money> = sumByCurrency(
    subscriptions
        .filter { it.status == SubscriptionStatus.ACTIVE }
        .map { it.monthlyEquivalent() }
)

/**
 * [month]에 결제되는 금액(해지 전 결제 포함)을 통화별로 합산합니다(통화 코드 순).
 *
 * 오버플로 여지: 구독 1,000건 * MAX_MINOR 1e12 * 월 최대 31회(일 단위 주기) = 3.1e16 이며
 * Long 최대값 약 9.22e18 보다 작습니다. 1,000건은 MAX_SUBSCRIPTIONS로 저장 경로(Task 8)에서 보장합니다.
 */
fun dueTotals(subscriptions: List<Subscription>, month: YearMonth): List<Money> = sumByCurrency(
    paymentsIn(subscriptions, month.atDay(1)..month.atEndOfMonth()).map {
        it.subscription.price
    }
)

private fun sumByCurrency(amounts: List<Money>): List<Money> = amounts
    .groupBy { it.currency.value }
    .toSortedMap()
    .map { (_, list) -> list.reduce(Money::plus) }

data class AmountSummary(val primary: Money, val others: List<Money>)

/** 기본 통화 합계를 primary로, 없으면 통화 코드 순 첫 통화, 아무것도 없으면 0. others는 0원 제외. */
fun summarize(totals: List<Money>, defaultCurrency: CurrencyCode): AmountSummary {
    val sorted = totals.sortedBy { it.currency.value }
    val primary = sorted.firstOrNull { it.currency == defaultCurrency }
        ?: sorted.firstOrNull()
        ?: Money(0, defaultCurrency)
    val others = sorted.filter { it.currency != primary.currency && it.minor != 0L }
    return AmountSummary(primary, others)
}
