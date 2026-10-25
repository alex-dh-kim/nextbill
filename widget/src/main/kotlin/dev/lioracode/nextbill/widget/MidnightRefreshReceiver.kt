package dev.lioracode.nextbill.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.domain.effect.WidgetUpdater
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
private const val TAG = "MidnightRefreshReceiver"

@AndroidEntryPoint
class MidnightRefreshReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var widget: WidgetUpdater

    @Inject lateinit var dayChange: DayChangeScheduler

    @Inject @field:ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != MIDNIGHT_ACTION) return
        val pending = goAsync()
        handleMidnight(scope, widget::updateAll, dayChange::scheduleNext, pending::finish)
    }
}

internal fun handleMidnight(
    scope: CoroutineScope,
    updateAll: suspend () -> Unit,
    scheduleNext: suspend () -> Unit,
    finish: () -> Unit,
    budgetMs: Long = MIDNIGHT_BUDGET_MS
): Job = scope.launch(start = CoroutineStart.ATOMIC) {
    try {
        withTimeout(budgetMs) {
            runCatchingNonCancellation { scheduleNext() }
                .onFailure { Log.w(TAG, "reschedule failed", it) }
            runCatchingNonCancellation { updateAll() }
                .onFailure { Log.w(TAG, "widget update failed", it) }
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "midnight refresh timed out", e)
    } finally {
        finish()
    }
}
