package dev.lioracode.nextbill.widget

import android.app.PendingIntent
import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.notifications.InexactAlarms
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.at
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private class RecordingAlarms : InexactAlarms {
    val windows = mutableListOf<Triple<PendingIntent, Instant, Long>>()
    val cancelled = mutableListOf<PendingIntent>()

    override fun setWindow(intent: PendingIntent, triggerAt: Instant, windowMillis: Long) {
        windows += Triple(intent, triggerAt, windowMillis)
    }

    override fun cancel(intent: PendingIntent) {
        cancelled += intent
    }
}

@RunWith(RobolectricTestRunner::class)
class MidnightRefreshTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val alarms = RecordingAlarms()

    private fun refresh(clock: MutableClock, hasWidgets: Boolean) = MidnightRefresh(
        context,
        alarms,
        clock,
        object : WidgetPresence {
            override suspend fun hasWidgets() = hasWidgets
        }
    )

    @Test
    fun midnightIsNextLocalMidnight() = runTest {
        refresh(MutableClock(at("2026-11-15T23:59:30+09:00")), true).scheduleNext()
        val (_, at, window) = alarms.windows.single()
        assertThat(at).isEqualTo(at("2026-11-16T00:00+09:00"))
        assertThat(window).isEqualTo(15 * 60_000L)
    }

    @Test
    fun midnightAcrossDst() = runTest {
        val ny = ZoneId.of("America/New_York")
        refresh(MutableClock(at("2027-03-13T12:00-05:00"), ny), true).scheduleNext()
        assertThat(alarms.windows.single().second).isEqualTo(at("2027-03-14T00:00-05:00"))
    }

    @Test
    fun usesInjectedClockZoneNotSystemDefault() = runTest {
        // 같은 순간이라도 시간대에 따라 다음 자정이 다릅니다.
        val instant = at("2026-11-15T12:00Z")
        refresh(MutableClock(instant, ZoneId.of("Asia/Seoul")), true).scheduleNext()
        refresh(MutableClock(instant, ZoneId.of("Pacific/Auckland")), true).scheduleNext()
        assertThat(alarms.windows.map { it.second }).containsExactly(
            at("2026-11-16T00:00+09:00"),
            at("2026-11-17T00:00+13:00") // Auckland은 이미 11/16 01:00
        ).inOrder()
    }

    @Test
    fun noWidgetMeansNoAlarm() = runTest {
        refresh(MutableClock(at("2026-11-15T10:00+09:00")), false).scheduleNext()
        assertThat(alarms.windows).isEmpty()
        assertThat(alarms.cancelled).hasSize(1)
    }

    @Test
    fun pendingIntentIsStable() = runTest {
        val clock = MutableClock(at("2026-11-15T10:00+09:00"))
        refresh(clock, true).scheduleNext()
        refresh(clock, false).scheduleNext()
        assertThat(alarms.cancelled.single()).isEqualTo(alarms.windows.single().first)
    }
}
