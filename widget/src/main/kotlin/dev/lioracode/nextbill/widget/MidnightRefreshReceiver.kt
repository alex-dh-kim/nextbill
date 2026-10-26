package dev.lioracode.nextbill.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.runCatchingNonCancellation
import dev.lioracode.nextbill.core.notifications.HiltBroadcastReceiver
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal const val MIDNIGHT_BUDGET_MS = 8_000L
private const val TAG = "NextBill"

/**
 * 자정 알람. 다음 자정을 먼저 다시 건 뒤 파생 상태 전체를 갱신합니다(알림 sync·지난 알림 정리 → 위젯).
 * 정리를 함께 돌려야 어제 결제일의 알림이 앱을 열기 전에도 사라집니다. 이미 떠 있던 프로세스의 날짜
 * 원천은 잠든 사이 자정 타이머가 늦거나 DATE_CHANGED가 이 알람보다 늦게 올 수 있으므로, 갱신 전에
 * 날짜를 다시 계산합니다.
 */
@AndroidEntryPoint
class MidnightRefreshReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var refresher: DerivedStateRefresher

    @Inject lateinit var dayChange: DayChangeScheduler

    @Inject lateinit var dateSource: CurrentDateSource

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != MIDNIGHT_ACTION) return
        val pending = goAsync()
        handleMidnight(
            scope = scope,
            recomputeDate = dateSource::refresh,
            refresh = refresher::refreshNow,
            scheduleNext = dayChange::scheduleNext,
            finish = pending::finish
        )
    }
}

/**
 * [recomputeDate] → [scheduleNext] → [refresh] 순서입니다. [scheduleNext]를 [refresh]보다 먼저 부르므로
 * [refresh]가 멈추거나 실패해도 다음 자정 알람은 남습니다.
 */
internal fun handleMidnight(
    scope: CoroutineScope,
    recomputeDate: () -> Unit,
    refresh: suspend () -> Unit,
    scheduleNext: suspend () -> Unit,
    finish: () -> Unit,
    budgetMs: Long = MIDNIGHT_BUDGET_MS
): Job = scope.launch(start = CoroutineStart.ATOMIC) {
    try {
        withTimeout(budgetMs) {
            runCatchingNonCancellation(recomputeDate)
                .onFailure { Log.w(TAG, "date recompute failed", it) }
            runCatchingNonCancellation { scheduleNext() }
                .onFailure { Log.w(TAG, "reschedule failed", it) }
            runCatchingNonCancellation { refresh() }
                .onFailure { Log.w(TAG, "midnight refresh failed", it) }
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "midnight refresh timed out", e)
    } finally {
        finish()
    }
}
