package dev.lioracode.nextbill.core.domain.time

import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/** 오늘 날짜(ADR-12). */
interface CurrentDateSource {
    val today: StateFlow<LocalDate>
}
