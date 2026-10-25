package dev.lioracode.nextbill.core.domain.reminder

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 결제일까지 남은 날 표시(오늘, 내일, D-n). */
sealed interface DueLabel {
    data object Today : DueLabel

    data object Tomorrow : DueLabel

    data class InDays(val days: Int) : DueLabel
}

/** [date] >= [today]를 전제합니다. */
fun dueLabel(date: LocalDate, today: LocalDate): DueLabel {
    require(date >= today) { "date must not be before today" }
    return when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> DueLabel.Today
        1L -> DueLabel.Tomorrow
        else -> DueLabel.InDays(Math.toIntExact(days))
    }
}
