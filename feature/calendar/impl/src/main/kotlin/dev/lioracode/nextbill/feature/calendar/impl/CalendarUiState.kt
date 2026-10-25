package dev.lioracode.nextbill.feature.calendar.impl

import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    /** 날짜별 최대 3개(결제 순서). */
    val dots: Map<LocalDate, List<ColorKey>>,
    /** 접근성 설명용 실제 결제 건수. */
    val paymentCounts: Map<LocalDate, Int>,
    val monthCount: Int,
    val monthTotal: AmountSummary,
    val selectedPayments: List<CalendarPayment>,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY
)

data class CalendarPayment(
    val id: SubscriptionId,
    val name: String,
    val colorKey: ColorKey,
    val price: Money,
    val cycle: BillingCycle,
    val anchor: LocalDate
)

/** 앞쪽 빈칸과 뒤쪽 빈칸(null)을 포함해 길이가 7의 배수인 달력 칸. */
fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(leading) { null } + days
    return cells + List<LocalDate?>((7 - cells.size % 7) % 7) { null }
}
