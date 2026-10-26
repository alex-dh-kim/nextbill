package dev.lioracode.nextbill.widget

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WidgetStatesTest {
    @Test
    fun recomputesWhenTheDateChanges() = runTest {
        val today = MutableStateFlow<LocalDate>(d("2026-11-15"))
        val states = widgetStates(
            today,
            flowOf(TestSubscriptions.all),
            flowOf(CurrencyCode.KRW)
        )
        val first = states.take(1).toList().single()
        today.value = d("2026-11-17")
        val second = states.take(1).toList().single()
        assertThat(first.rows.first().label).isEqualTo(DueLabel.Today)
        assertThat(second.rows.first().label).isNotEqualTo(DueLabel.Today)
    }
}
