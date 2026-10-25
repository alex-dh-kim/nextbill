package dev.lioracode.nextbill.feature.calendar.impl

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class MonthGridTest {
    private val nov = YearMonth.of(2026, 11)

    @Test fun monthCellsNov2026SundayFirst() {
        val cells = monthCells(nov, DayOfWeek.SUNDAY)
        assertThat(cells).hasSize(35)
        assertThat(cells.first()).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(cells.take(30).all { it != null }).isTrue()
        assertThat(cells.drop(30).all { it == null }).isTrue()
    }

    @Test fun monthCellsMondayFirst() {
        val cells = monthCells(nov, DayOfWeek.MONDAY)
        assertThat(cells).hasSize(42)
        assertThat(cells.take(6).all { it == null }).isTrue()
        assertThat(cells[6]).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    @Test fun lengthIsAlwaysMultipleOfSeven() {
        for (m in 1..12) {
            for (d in DayOfWeek.entries) {
                assertThat(monthCells(YearMonth.of(2026, m), d).size % 7).isEqualTo(0)
            }
        }
    }
}
