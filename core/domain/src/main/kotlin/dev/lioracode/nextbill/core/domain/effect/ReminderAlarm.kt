package dev.lioracode.nextbill.core.domain.effect

import java.time.Instant

/** 앱 전체에 OS 알람 하나. 상태 저장은 하지 않습니다. */
interface ReminderAlarm {
    fun setNext(at: Instant)

    fun cancel()
}
