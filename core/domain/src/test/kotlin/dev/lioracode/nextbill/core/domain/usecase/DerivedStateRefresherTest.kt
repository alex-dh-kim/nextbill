@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.tag
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.TestSubscriptions.phoneBill
import dev.lioracode.nextbill.core.testing.TestSubscriptions.youtube
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

class DerivedStateRefresherTest {
    @Test fun saveSucceedsEvenWhenAlarmFails() = runTest {
        val env = TestEnv(appScope())
        env.startup.start()
        advanceUntilIdle()
        env.alarm.failNext = SecurityException("alarm denied")

        val result = env.save(netflix.newDraft(), 0)
        advanceUntilIdle()

        assertThat(result).isEqualTo(Outcome.Ok(SubscriptionId(1)))
        assertThat(env.repo.getAll()).hasSize(1)
        assertThat(env.scheduled.snapshot).containsKey(SubscriptionId(1))
        assertThat(env.alarm.nextAt).isNull()
    }

    @Test fun cancellationIsNotSwallowed() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.markReady()
        env.scheduled.callDelay = 1.seconds
        val caught = CompletableDeferred<Throwable?>()
        val job = launch {
            try {
                env.refresher.refreshNow()
                caught.complete(null)
            } catch (e: Throwable) {
                caught.complete(e)
                throw e
            }
        }
        advanceTimeBy(500.milliseconds)

        job.cancel()
        job.join()

        assertThat(caught.await()).isInstanceOf(CancellationException::class.java)
        assertThat(env.dayChange.count).isEqualTo(0)
        assertThat(env.widget.updateCount).isEqualTo(0)
    }

    @Test fun cancelledScopeStopsRequestedRuns() = runTest {
        val scope = appScope()
        val env = TestEnv(scope, listOf(netflix))
        env.markReady()
        env.scheduled.callDelay = 1.seconds

        env.refresher.request()
        runCurrent()
        advanceTimeBy(500.milliseconds)
        scope.cancel()
        advanceUntilIdle()

        assertThat(env.dayChange.count).isEqualTo(0)
        assertThat(env.widget.updateCount).isEqualTo(0)
    }

    /** 단계 안의 취소 예외(시간 초과)는 그 단계의 실패일 뿐, 요청 루프를 끝내지 않습니다. */
    @Test fun stepTimeoutDoesNotStopLaterRequests() = runTest {
        val env = TestEnv(appScope())
        env.markReady()
        val timeout = runCatching {
            withTimeout(1.milliseconds) { awaitCancellation() }
        }.exceptionOrNull()
        assertThat(timeout).isInstanceOf(TimeoutCancellationException::class.java)
        env.widget.failWith = timeout

        env.refresher.request()
        advanceUntilIdle()
        env.widget.failWith = null
        env.refresher.request()
        advanceUntilIdle()

        assertThat(env.widget.updateCount).isEqualTo(2)
        assertThat(env.dayChange.count).isEqualTo(2)
    }

    @Test fun widgetFailureDoesNotSkipDayChange() = runTest {
        val env = TestEnv(appScope())
        env.markReady()
        env.widget.failWith = IllegalStateException("widget")

        env.refresher.refreshNow()

        assertThat(env.dayChange.count).isEqualTo(1)
        assertThat(env.widget.updateCount).isEqualTo(1)
    }

    @Test fun requestsAreSerializedAndRunWithLatestData() = runTest {
        val env = TestEnv(appScope())
        env.markReady()
        env.scheduled.callDelay = 1.seconds

        env.repo.upsert(netflix)
        env.refresher.request()
        runCurrent()
        env.repo.upsert(phoneBill)
        env.refresher.request()
        env.repo.upsert(youtube)
        env.refresher.request()
        advanceUntilIdle()

        assertThat(env.dayChange.count).isIn(1..2)
        assertThat(env.scheduled.snapshot.keys)
            .containsExactly(netflix.id, phoneBill.id, youtube.id)
    }

    /** 정리는 지난 결제일뿐 아니라 삭제·해지된 구독의 알림도 지웁니다(planShown과 같은 규칙). */
    @Test fun pruneDropsNotificationsOfDeletedOrCancelledSubscriptions() = runTest {
        val env = TestEnv(appScope(), listOf(netflix, phoneBill, youtube))
        env.markReady()
        val date = env.today.plusDays(3)
        val posts = listOf(netflix, phoneBill, youtube).map { ReminderPost.Single(it, date) }
        env.poster.show(posts, env.today)

        env.repo.delete(netflix.id)
        val ended = phoneBill.periods.map { it.copy(end = it.end ?: env.today) }
        env.repo.upsert(phoneBill.copy(periods = ended))
        check(env.repo.get(phoneBill.id)?.status == SubscriptionStatus.CANCELLED)
        env.refresher.refreshNow()

        assertThat(env.poster.active.keys).containsExactly(posts[2].tag)
    }

    /** 격리한 단계 실패는 Error까지 reporter에 알리고, 다음 단계는 그대로 실행합니다. */
    @Test fun stepFailuresAreReported() = runTest {
        val env = TestEnv(appScope())
        env.markReady()
        env.widget.failWith = NoSuchMethodError("LocalDate.ofInstant")

        env.refresher.refreshNow()

        assertThat(env.failures.map { it.first }).containsExactly("refresh widget")
        assertThat(env.failures.single().second).isInstanceOf(NoSuchMethodError::class.java)
        assertThat(env.dayChange.count).isEqualTo(1)
    }
}
