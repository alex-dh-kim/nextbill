package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeCurrentDateSource(initial: LocalDate) : CurrentDateSource {
    private val state = MutableStateFlow(initial)
    override val today: StateFlow<LocalDate> = state

    /** [refresh]가 옮겨 갈 날짜(진짜의 `LocalDate.now(clock)` 대신). null이면 [refresh]는 아무것도 하지 않습니다. */
    @Volatile var dateOnRefresh: LocalDate? = null

    fun set(date: LocalDate) {
        state.value = date
    }

    override fun refresh() {
        dateOnRefresh?.let { state.value = it }
    }
}
