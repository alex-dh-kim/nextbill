package dev.lioracode.nextbill.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.lioracode.nextbill.core.domain.usecase.runCatchingNonCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "NextBill"
internal const val WIDGET_SCHEDULE_BUDGET_MS = 8_000L

/** 첫 위젯이 놓이면 자정 알람을 걸고, 마지막 위젯이 지워지면 취소합니다(위젯 유무는 스케줄러가 판단). */
class NextBillWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextBillWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        rescheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        rescheduleMidnight(context)
    }

    private fun rescheduleMidnight(context: Context) {
        val entry = context.widgetEntryPoint()
        val pending = goAsync()
        scheduleWithin(entry.applicationScope(), entry.dayChange()::scheduleNext, pending::finish)
    }
}

internal fun scheduleWithin(
    scope: CoroutineScope,
    scheduleNext: suspend () -> Unit,
    finish: () -> Unit,
    budgetMs: Long = WIDGET_SCHEDULE_BUDGET_MS
): Job = scope.launch(start = CoroutineStart.ATOMIC) {
    try {
        withTimeout(budgetMs) {
            runCatchingNonCancellation { scheduleNext() }
                .onFailure { Log.w(TAG, "midnight reschedule failed", it) }
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "midnight reschedule timed out", e)
    } finally {
        finish()
    }
}
