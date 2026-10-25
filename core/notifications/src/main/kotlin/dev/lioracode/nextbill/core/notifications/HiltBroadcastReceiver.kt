package dev.lioracode.nextbill.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Kotlin은 [BroadcastReceiver.onReceive]가 추상이라 `@AndroidEntryPoint` 수신기에서 `super.onReceive`를
 * 부를 수 없습니다. Hilt는 이 클래스를 상속한 `Hilt_*`를 만들어 `onReceive`에서 주입한 뒤 `super`를 부르므로,
 * 구체 `onReceive`를 가진 중간 클래스를 둡니다.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
