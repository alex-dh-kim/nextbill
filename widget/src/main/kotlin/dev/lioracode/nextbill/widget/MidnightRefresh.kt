package dev.lioracode.nextbill.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.notifications.InexactAlarms
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

internal const val MIDNIGHT_ACTION = "dev.lioracode.nextbill.MIDNIGHT"
private const val MIDNIGHT_URI = "nextbill://midnight"
private const val MIDNIGHT_REQUEST_CODE = 0
internal const val MIDNIGHT_WINDOW_MILLIS = 15 * 60_000L

/** 다음 현지 자정 시각. 시간대는 주입된 [clock]의 것을 씁니다. */
internal fun nextLocalMidnight(clock: Clock): Instant {
    val now = ZonedDateTime.now(clock)
    return now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant()
}

/**
 * 앱의 두 번째이자 마지막 OS 알람. 위젯이 있을 때만 다음 자정에 15분 창으로 겁니다. 전달은 best effort입니다.
 */
class MidnightRefresh @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarms: InexactAlarms,
    private val clock: Clock,
    private val presence: WidgetPresence
) : DayChangeScheduler {
    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, MidnightRefreshReceiver::class.java)
            .setAction(MIDNIGHT_ACTION)
            .setData(MIDNIGHT_URI.toUri())
        return PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override suspend fun scheduleNext() {
        if (presence.hasWidgets()) {
            alarms.setWindow(pendingIntent(), nextLocalMidnight(clock), MIDNIGHT_WINDOW_MILLIS)
        } else {
            alarms.cancel(pendingIntent())
        }
    }
}
