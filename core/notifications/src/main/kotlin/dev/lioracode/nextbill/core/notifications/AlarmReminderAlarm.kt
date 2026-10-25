package dev.lioracode.nextbill.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.effect.ReminderAlarm
import java.time.Instant
import javax.inject.Inject

internal const val REMINDER_ACTION = "dev.lioracode.nextbill.REMINDER"
private const val REMINDER_URI = "nextbill://reminder/next"
private const val REMINDER_REQUEST_CODE = 0
internal const val REMINDER_WINDOW_MILLIS = 3_600_000L

/**
 * 앱 전체에 OS 알람 하나. 같은 PendingIntent(액션·data·requestCode 고정)를 다시 걸면 이전 알람을 대체합니다.
 * extras가 없습니다. 무엇을 알릴지는 수신 시점에 DB 기록으로 정합니다. 전달은 best effort입니다.
 */
class AlarmReminderAlarm @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarms: InexactAlarms
) : ReminderAlarm {
    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(REMINDER_ACTION)
            .setData(REMINDER_URI.toUri())
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun setNext(at: Instant) {
        alarms.setWindow(pendingIntent(), at, REMINDER_WINDOW_MILLIS)
    }

    override fun cancel() {
        alarms.cancel(pendingIntent())
    }
}
