package dev.lioracode.nextbill.core.data.time

import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.d
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AndroidCurrentDateSourceTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")

    /** 가상 시간(TestScheduler.currentTime)과 함께 흐르는 시계. */
    private class VirtualClock(
        private val scope: TestScope,
        private val start: Instant,
        var zoneId: ZoneId
    ) : Clock() {
        override fun getZone(): ZoneId = zoneId

        override fun withZone(zone: ZoneId): Clock = VirtualClock(scope, start, zone)

        override fun instant(): Instant = start.plusMillis(scope.testScheduler.currentTime)
    }

    @Test
    fun dateSourceEmitsOnDateChangedBroadcast() = runTest {
        val clock = MutableClock(zoneId = seoul).also { it.setDate(d("2026-11-15")) }
        val source = AndroidCurrentDateSource(context, clock, backgroundScope)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
        clock.setDate(d("2026-11-16"))
        context.sendBroadcast(Intent(Intent.ACTION_DATE_CHANGED))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(source.today.value).isEqualTo(d("2026-11-16"))
    }

    @Test
    fun dateSourceEmitsAtMidnightWhileRunning() = runTest {
        val start = d("2026-11-15").atTime(23, 59, 30).atZone(seoul).toInstant()
        val clock = VirtualClock(this, start, seoul)
        val source = AndroidCurrentDateSource(context, clock, backgroundScope)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
        advanceTimeBy(29_000)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
        advanceTimeBy(2_000)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-16"))
    }

    @Test
    fun dateSourceEmitsOnTimezoneChange() = runTest {
        // 2026-11-15 01:00 Seoul == 2026-11-14 11:00 New York
        val clock = MutableClock(
            instant = d("2026-11-15").atTime(1, 0).atZone(seoul).toInstant(),
            zoneId = seoul
        )
        val source = AndroidCurrentDateSource(context, clock, backgroundScope)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
        clock.zoneId = newYork
        context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(source.today.value).isEqualTo(d("2026-11-14"))
    }

    @Test
    fun broadcastRearmsMidnightWait() = runTest {
        val start = d("2026-11-15").atTime(1, 0).atZone(seoul).toInstant()
        val clock = VirtualClock(this, start, seoul)
        val source = AndroidCurrentDateSource(context, clock, backgroundScope)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
        // 서울 01:00 == 뉴욕 11-14 11:00. 뉴욕 자정까지 13시간.
        clock.zoneId = newYork
        context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-14"))
        advanceTimeBy(13L * 3_600_000 - 1_000)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-14"))
        advanceTimeBy(2_000)
        runCurrent()
        assertThat(source.today.value).isEqualTo(d("2026-11-15"))
    }
}
