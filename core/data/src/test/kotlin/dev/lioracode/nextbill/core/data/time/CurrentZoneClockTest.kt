package dev.lioracode.nextbill.core.data.time

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Test

class CurrentZoneClockTest {
    @Test
    fun currentZoneClockFollowsDefaultZoneChanges() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
            assertThat(CurrentZoneClock.zone).isEqualTo(ZoneId.of("Asia/Seoul"))
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertThat(CurrentZoneClock.zone).isEqualTo(ZoneId.of("America/New_York"))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
