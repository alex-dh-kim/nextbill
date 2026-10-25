package dev.lioracode.nextbill.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** 이 앱의 알림 설정 화면을 엽니다. 설정 화면이 없는 기기에서는 아무 일도 하지 않습니다. */
fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 할 수 있는 일이 없으므로 무시합니다.
    }
}
