package dev.lioracode.nextbill.core.testing

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MutableClock(
    var instant: Instant = Instant.parse("2026-11-14T01:00:00Z"),
    var zoneId: ZoneId = SEOUL
) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    /** [date]의 [time](기본 10:00, 현재 시간대)로 맞춥니다. */
    fun setDate(date: LocalDate, time: String = "10:00") {
        instant = date.atTime(LocalTime.parse(time)).atZone(zoneId).toInstant()
    }
}
