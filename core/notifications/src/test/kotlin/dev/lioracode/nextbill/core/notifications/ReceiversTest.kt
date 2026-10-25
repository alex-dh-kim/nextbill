@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.notifications

import android.app.AlarmManager
import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.testing.FakeReminderAlarm
import dev.lioracode.nextbill.core.testing.FakeScheduledReminderRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.at
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReceiversTest {
    private val events = mutableListOf<String>()

    @Test
    fun reminderReceiverDelegates() = runTest {
        handleReminderAlarm(
            scope = this,
            onAlarmFired = { events += "fired" },
            deliver = { events += "deliver" },
            requestRefresh = { events += "request" },
            finish = { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("fired", "deliver", "request", "finish").inOrder()
    }

    @Test
    fun receiverFinishesWhenDeliverHangs() = runTest {
        val clock = MutableClock(at("2026-11-14T09:00+09:00"))
        val alarm = FakeReminderAlarm()
        val sync = SyncRemindersUseCase(
            FakeSubscriptionRepository(),
            FakeScheduledReminderRepository(),
            alarm,
            clock
        )
        handleReminderAlarm(
            scope = this,
            onAlarmFired = sync::onAlarmFired,
            deliver = { awaitCancellation() },
            requestRefresh = { events += "request" },
            finish = { events += "finish" }
        )
        runCurrent()
        advanceTimeBy(DELIVERY_BUDGET_MS - 1)
        assertThat(events).isEmpty()
        advanceTimeBy(1)
        runCurrent()
        assertThat(events).containsExactly("request", "finish").inOrder()
        // 재시도 알람(15분 뒤)이 남아 있습니다.
        assertThat(alarm.nextAt).isEqualTo(clock.instant().plus(SyncRemindersUseCase.RETRY_DELAY))
    }

    @Test
    fun deliverFailureDoesNotEscapeAndStillRequestsRefresh() = runTest {
        handleReminderAlarm(
            scope = this,
            onAlarmFired = {},
            deliver = { throw IOException("boom") },
            requestRefresh = { events += "request" },
            finish = { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("request", "finish").inOrder()
    }

    @Test
    fun finishesEvenIfAlarmBookkeepingFails() = runTest {
        handleReminderAlarm(
            scope = this,
            onAlarmFired = { error("alarm") },
            deliver = { events += "deliver" },
            requestRefresh = { events += "request" },
            finish = { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("deliver", "request", "finish").inOrder()
    }

    @Test
    fun finishRunsEvenIfScopeIsAlreadyCancelled() = runTest {
        val dead = CoroutineScope(Job().apply { cancel() } + StandardTestDispatcher(testScheduler))
        handleReminderAlarm(
            scope = dead,
            onAlarmFired = {},
            deliver = { events += "deliver" },
            requestRefresh = { events += "request" },
            finish = { events += "finish" }
        )
        handleReschedule(dead, { events += "refresh" }, { events += "finish2" })
        runCurrent()
        // 취소된 scope에서도 finish는 정확히 한 번씩 부르고, 시작된 본문은 첫 중단점에서 취소됩니다.
        assertThat(events.filter { it.startsWith("finish") }).containsExactly("finish", "finish2")
        assertThat(events.count { it == "request" }).isAtMost(1)
    }

    @Test
    fun rescheduleReceiverRefreshes() = runTest {
        handleReschedule(this, { events += "refresh" }, { events += "finish" })
        runCurrent()
        assertThat(events).containsExactly("refresh", "finish").inOrder()
    }

    @Test
    fun rescheduleFinishesOnFailureAndTimeout() = runTest {
        handleReschedule(this, { throw IOException("x") }, { events += "finish1" })
        runCurrent()
        handleReschedule(this, { awaitCancellation() }, { events += "finish2" })
        runCurrent()
        advanceTimeBy(RESCHEDULE_BUDGET_MS)
        runCurrent()
        assertThat(events).containsExactly("finish1", "finish2").inOrder()
    }

    @Test
    fun handledActionsAreTheFourSystemBroadcasts() {
        assertThat(RESCHEDULE_ACTIONS).containsExactly(
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.intent.action.MY_PACKAGE_REPLACED"
        )
    }
}
