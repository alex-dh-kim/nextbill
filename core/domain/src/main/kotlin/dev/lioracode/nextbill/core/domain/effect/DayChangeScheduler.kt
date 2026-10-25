package dev.lioracode.nextbill.core.domain.effect

interface DayChangeScheduler {
    /** 위젯이 없으면 예약을 취소합니다. */
    suspend fun scheduleNext()
}
