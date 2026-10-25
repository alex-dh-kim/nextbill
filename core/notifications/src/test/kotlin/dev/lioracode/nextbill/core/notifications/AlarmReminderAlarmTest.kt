package dev.lioracode.nextbill.core.notifications

import android.app.AlarmManager
import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AlarmReminderAlarmTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarm = AlarmReminderAlarm(context, AndroidInexactAlarms(context))

    @Before
    fun clean() {
        shadowOf(manager).scheduledAlarms.toList().forEach { manager.cancel(it.operation!!) }
    }

    @Test
    fun setsOneInexactWindowAlarm() {
        alarm.setNext(Instant.parse("2026-11-14T00:00:00Z"))
        alarm.setNext(Instant.parse("2026-11-15T00:00:00Z"))

        val alarms = shadowOf(manager).scheduledAlarms
        assertThat(alarms).hasSize(1)
        val scheduled = alarms.single()
        assertThat(scheduled.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(
            scheduled.triggerAtMs
        ).isEqualTo(Instant.parse("2026-11-15T00:00:00Z").toEpochMilli())
        assertThat(scheduled.windowLengthMs).isEqualTo(3_600_000L)
        val intent = shadowOf(scheduled.operation).savedIntent
        assertThat(intent.action).isEqualTo("dev.lioracode.nextbill.REMINDER")
        assertThat(intent.data.toString()).isEqualTo("nextbill://reminder/next")
        assertThat(intent.extras).isNull()
        assertThat(shadowOf(scheduled.operation).requestCode).isEqualTo(0)
    }

    @Test
    fun cancelRemovesAlarm() {
        alarm.setNext(Instant.parse("2026-11-14T00:00:00Z"))
        alarm.cancel()
        assertThat(shadowOf(manager).scheduledAlarms).isEmpty()
    }

    @Test
    fun cancelWithoutAlarmIsHarmless() {
        alarm.cancel()
        assertThat(shadowOf(manager).scheduledAlarms).isEmpty()
    }
}
