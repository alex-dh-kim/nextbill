package dev.lioracode.nextbill.core.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal const val RESCHEDULE_BUDGET_MS = 8_000L

internal val RESCHEDULE_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_MY_PACKAGE_REPLACED
)

private const val TAG = "NextBill"

/**
 * 부팅·시간·시간대 변경·앱 업데이트 뒤 알림 알람을 다시 맞춥니다. 부팅·업데이트는 새 프로세스라 첫 sync가
 * 알람을 다시 겁니다.
 */
@AndroidEntryPoint
class RescheduleReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var refresher: DerivedStateRefresher

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val pending = goAsync()
        handleReschedule(scope, refresher::refreshNow, pending::finish)
    }
}

internal fun handleReschedule(
    scope: CoroutineScope,
    refresh: suspend () -> Unit,
    finish: () -> Unit,
    budgetMs: Long = RESCHEDULE_BUDGET_MS
): Job = scope.launch(start = CoroutineStart.ATOMIC) {
    try {
        withTimeout(budgetMs) { refresh() }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "refresh timed out", e)
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.w(TAG, "refresh failed", e)
    } finally {
        finish()
    }
}
