package dev.lioracode.nextbill.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/** 비정확 알람 창. 정확 알람 권한을 쓰지 않습니다(설계 문서 ADR-3). */
interface InexactAlarms {
    fun setWindow(intent: PendingIntent, triggerAt: Instant, windowMillis: Long)

    fun cancel(intent: PendingIntent)
}

class AndroidInexactAlarms @Inject constructor(@ApplicationContext private val context: Context) :
    InexactAlarms {
    private val manager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    override fun setWindow(intent: PendingIntent, triggerAt: Instant, windowMillis: Long) {
        manager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), windowMillis, intent)
    }

    override fun cancel(intent: PendingIntent) {
        manager.cancel(intent)
    }
}
