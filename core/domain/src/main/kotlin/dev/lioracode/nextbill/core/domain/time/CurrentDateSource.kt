package dev.lioracode.nextbill.core.domain.time

import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/** 오늘 날짜(ADR-12). */
interface CurrentDateSource {
    val today: StateFlow<LocalDate>

    /**
     * 주입한 Clock으로 [today]를 지금 다시 계산합니다. 자정 알람처럼 날짜가 막 바뀌었을 때, 자체 타이머·
     * 브로드캐스트보다 먼저 불렸어도 새 날짜로 처리하게 합니다.
     */
    fun refresh()
}
