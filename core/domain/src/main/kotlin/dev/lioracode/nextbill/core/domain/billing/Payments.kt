@file:Suppress("ktlint:standard:filename")

package dev.lioracode.nextbill.core.domain.billing

import dev.lioracode.nextbill.core.model.Subscription
import java.time.LocalDate

data class Payment(val subscription: Subscription, val date: LocalDate)

/** [range] 안의 모든 결제를 날짜 → 이름 순으로 돌려줍니다. 과거 날짜도 포함합니다. */
fun paymentsIn(subscriptions: List<Subscription>, range: ClosedRange<LocalDate>): List<Payment> =
    subscriptions
        .flatMap { s ->
            s.billingDatesFrom(range.start)
                .takeWhile { it <= range.endInclusive }
                .map { Payment(s, it) }
                .toList()
        }
        .sortedWith(compareBy({ it.date }, { it.subscription.name }, { it.subscription.id.value }))
