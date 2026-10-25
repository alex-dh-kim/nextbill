package dev.lioracode.nextbill.widget

import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.domain.billing.dueTotals
import dev.lioracode.nextbill.core.domain.billing.nextBillingDate
import dev.lioracode.nextbill.core.domain.billing.paymentsIn
import dev.lioracode.nextbill.core.domain.billing.summarize
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate
import java.time.YearMonth

const val WIDGET_MAX_ROWS = 3

data class WidgetRow(
    val name: String,
    val colorKey: ColorKey,
    val price: Money,
    val date: LocalDate,
    val label: DueLabel
)

data class WidgetState(
    val monthTotal: AmountSummary,
    val rows: List<WidgetRow>,
    val upcomingCount: Int
)

/** 위젯에 그릴 값을 순수 함수로 만듭니다. */
fun widgetState(
    subscriptions: List<Subscription>,
    today: LocalDate,
    defaultCurrency: CurrencyCode
): WidgetState {
    val month = YearMonth.from(today)
    val rows = subscriptions
        .filter { it.status == SubscriptionStatus.ACTIVE }
        .mapNotNull { s -> s.nextBillingDate(today)?.let { s to it } }
        .sortedWith(compareBy({ it.second }, { it.first.name }, { it.first.id.value }))
        .take(WIDGET_MAX_ROWS)
        .map { (s, date) -> WidgetRow(s.name, s.colorKey, s.price, date, dueLabel(date, today)) }
    return WidgetState(
        monthTotal = summarize(dueTotals(subscriptions, month), defaultCurrency),
        rows = rows,
        upcomingCount = paymentsIn(subscriptions, today..month.atEndOfMonth()).size
    )
}
