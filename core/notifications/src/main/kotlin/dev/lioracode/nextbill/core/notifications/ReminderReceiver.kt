package dev.lioracode.nextbill.core.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.usecase.DeliverDueRemindersUseCase
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.domain.usecase.runCatchingNonCancellation
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal const val DELIVERY_BUDGET_MS = 8_000L

private const val TAG = "NextBill"

/**
 * 알람 수신. 처리는 모두 use case에 맡깁니다. 시간 제한을 넘기면 취소되고, 소비하지 않은 기록은 남으며
 * [SyncRemindersUseCase.onAlarmFired]가 먼저 걸어 둔 재시도 알람이 이어서 처리합니다(best effort).
 */
@AndroidEntryPoint
class ReminderReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var sync: SyncRemindersUseCase

    @Inject lateinit var deliver: DeliverDueRemindersUseCase

    @Inject lateinit var refresher: DerivedStateRefresher

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != REMINDER_ACTION) return
        val pending = goAsync()
        handleReminderAlarm(
            scope = scope,
            onAlarmFired = sync::onAlarmFired,
            deliver = { deliver() },
            requestRefresh = refresher::request,
            finish = pending::finish
        )
    }
}

/**
 * [onAlarmFired]를 먼저 부르고(실패해도 계속), [scope]에서 [deliver]를 [budgetMs] 안에 실행합니다.
 * [deliver]의 취소 아닌 예외는 밖으로 나가지 않고, 어떤 경우에도 [requestRefresh] 뒤 [finish]를 부릅니다.
 * `finally`에는 멈추지 않는 일만 둡니다.
 */
// ATOMIC: 시작 전에 취소돼도 본문이 실행되어 finally의 finish()가 반드시 불립니다(goAsync 결과를 남기지 않기 위해).
@OptIn(DelicateCoroutinesApi::class)
internal fun handleReminderAlarm(
    scope: CoroutineScope,
    onAlarmFired: () -> Unit,
    deliver: suspend () -> Unit,
    requestRefresh: () -> Unit,
    finish: () -> Unit,
    budgetMs: Long = DELIVERY_BUDGET_MS
): Job {
    runCatchingNonCancellation(onAlarmFired).onFailure { Log.w(TAG, "onAlarmFired failed", it) }
    return scope.launch(start = CoroutineStart.ATOMIC) {
        try {
            withTimeout(budgetMs) { deliver() }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "delivery timed out; the retry alarm continues", e)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "delivery failed", e)
        } finally {
            try {
                runCatchingNonCancellation(requestRefresh)
                    .onFailure { Log.w(TAG, "refresh request failed", it) }
            } finally {
                finish()
            }
        }
    }
}
