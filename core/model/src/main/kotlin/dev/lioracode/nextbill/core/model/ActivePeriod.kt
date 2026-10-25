package dev.lioracode.nextbill.core.model

import java.time.LocalDate

/** 사용 구간. [end]는 포함하고, null이면 사용 중입니다. */
data class ActivePeriod(val start: LocalDate, val end: LocalDate?) {
    init {
        require(end == null || end >= start) { "end must not be before start" }
    }
}
