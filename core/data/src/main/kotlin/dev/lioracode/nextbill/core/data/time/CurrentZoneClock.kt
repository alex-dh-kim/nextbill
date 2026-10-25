package dev.lioracode.nextbill.core.data.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** 시스템 시간대가 바뀌면 다음 호출부터 새 시간대를 씁니다([Clock.systemDefaultZone]은 처음 값을 고정). */
object CurrentZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = systemUTC().instant()
}
