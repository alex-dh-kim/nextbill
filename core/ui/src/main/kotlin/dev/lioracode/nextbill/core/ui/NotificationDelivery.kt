package dev.lioracode.nextbill.core.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** 알림이 실제로 도착할 수 있는지(ADR-5). 사용자가 켠 알림 의도와는 별개입니다. */
sealed interface NotificationDelivery {
    data object Available : NotificationDelivery

    /** [canAsk]가 false면 시스템 대화상자가 더 뜨지 않을 수 있으니 설정으로 안내합니다. */
    data class NeedsPermission(val canAsk: Boolean) : NotificationDelivery

    data object BlockedInSettings : NotificationDelivery
}

fun notificationDelivery(
    sdk: Int,
    permissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    channelBlocked: Boolean,
    requestedBefore: Boolean,
    shouldShowRationale: Boolean
): NotificationDelivery = when {
    sdk >= 33 && !permissionGranted ->
        NotificationDelivery.NeedsPermission(canAsk = !requestedBefore || shouldShowRationale)

    !appNotificationsEnabled || channelBlocked -> NotificationDelivery.BlockedInSettings

    else -> NotificationDelivery.Available
}

// core:notifications의 REMINDER_CHANNEL_ID와 같은 값. core:ui는 그 모듈에 의존할 수 없어 복제합니다.
private const val REMINDER_CHANNEL_ID = "payment_reminders"

/** [requestedBefore]: OS 권한 대화상자를 실제로 띄운 적이 있는지. ON_RESUME마다 다시 계산합니다. */
@Composable
fun rememberNotificationDelivery(requestedBefore: Boolean): State<NotificationDelivery> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(computeDelivery(context, requestedBefore)) }
    DisposableEffect(lifecycleOwner, context, requestedBefore) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = computeDelivery(context, requestedBefore)
            }
        }
        state.value = computeDelivery(context, requestedBefore)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

private fun computeDelivery(context: Context, requestedBefore: Boolean): NotificationDelivery {
    val sdk = Build.VERSION.SDK_INT
    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val rationale =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } == true
    val manager = NotificationManagerCompat.from(context)
    val channel = manager.getNotificationChannel(REMINDER_CHANNEL_ID)
    val channelBlocked =
        channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
    return notificationDelivery(
        sdk = sdk,
        permissionGranted = granted,
        appNotificationsEnabled = manager.areNotificationsEnabled(),
        channelBlocked = channelBlocked,
        requestedBefore = requestedBefore,
        shouldShowRationale = rationale
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
