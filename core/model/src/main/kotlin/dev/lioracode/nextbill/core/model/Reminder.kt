package dev.lioracode.nextbill.core.model

import java.time.LocalTime

/** 결제일 알림 설정. [daysBefore]는 [DAYS_BEFORE_OPTIONS] 중 하나(0 = 당일). */
data class Reminder(val enabled: Boolean, val daysBefore: Int, val time: LocalTime) {
    companion object {
        val DAYS_BEFORE_OPTIONS = listOf(0, 1, 3, 7)
        val DEFAULT = Reminder(true, 1, LocalTime.of(9, 0))
    }
}
