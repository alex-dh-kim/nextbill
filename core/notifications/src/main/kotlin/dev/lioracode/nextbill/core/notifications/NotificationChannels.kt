package dev.lioracode.nextbill.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

const val REMINDER_CHANNEL_ID = "payment_reminders"

/**
 * 결제 알림 채널을 만듭니다. 이미 있으면 사용자가 바꾼 설정을 건드리지 않습니다(이름만 현재 언어로 갱신).
 * 앱 시작 때 부르고(Task 17), 게시 직전에도 늦게 한 번 더 부릅니다.
 */
fun ensureChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.notification_channel_reminders),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
}
