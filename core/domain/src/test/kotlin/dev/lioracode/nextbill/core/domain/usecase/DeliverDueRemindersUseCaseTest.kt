@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.tag
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.TestSubscriptions.phoneBill
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.io.IOException
import java.time.LocalTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class DeliverDueRemindersUseCaseTest {
    private val nov14At0800 = at("2026-11-14T08:00+09:00")
    private val nov14At1000 = at("2026-11-14T10:00+09:00")
    private val netflixNov = reminderOf(1, "2026-11-15", "2026-11-14T09:00+09:00")

    @Test fun deliversAllDueAndRearms() = runTest {
        val env = TestEnv(appScope(), listOf(netflix, phoneBill), now = nov14At0800)
        env.startup.start()
        advanceUntilIdle()
        env.setNow(at("2026-11-15T10:00+09:00"))

        assertThat(env.fire()).isEqualTo(Admission.Ran(2))

        assertThat(env.poster.posted).containsExactly(
            ReminderPost.Single(netflix, d("2026-11-15")),
            ReminderPost.Single(phoneBill, d("2026-11-16"))
        )
        assertThat(env.scheduled.snapshot).containsExactly(
            netflix.id,
            reminderOf(1, "2026-12-15", "2026-12-14T09:00+09:00"),
            phoneBill.id,
            reminderOf(2, "2026-12-16", "2026-12-15T09:00+09:00")
        )
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-12-14T09:00+09:00"))
    }

    @Test fun fourOrMoreDueBecomeOneSummary() = runTest {
        val subs = (1L..5L).map { sub(id = it, name = "S$it") }
        val env = TestEnv(appScope(), subs, now = nov14At0800)
        env.startup.start()
        advanceUntilIdle()
        env.setNow(nov14At1000)

        assertThat(env.fire()).isEqualTo(Admission.Ran(5))

        val summary = env.poster.posted.single() as ReminderPost.Summary
        assertThat(summary.items.map { it.first.id }).containsExactlyElementsIn(subs.map { it.id })
        assertThat(env.poster.active).hasSize(1)
    }

    @Test fun fiftyOneDeliveredOneAtATimeStayWithinCap() = runTest {
        val nine = LocalTime.of(9, 0)
        val subs = (0L..50L).map {
            sub(
                id = it + 1,
                name = "S%02d".format(it),
                reminder = Reminder(true, 1, nine.plusMinutes(it))
            )
        }
        val env = TestEnv(appScope(), subs, now = nov14At0800)
        env.startup.start()
        advanceUntilIdle()

        (0L..50L).forEach {
            env.setNow(at("2026-11-14T09:00+09:00").plusSeconds(it * 60))
            assertThat(env.fire()).isEqualTo(Admission.Ran(1))
        }

        assertThat(env.poster.rejected).isEmpty()
        val summary = env.poster.active.values.single() as ReminderPost.Summary
        assertThat(summary.items).hasSize(51)
    }

    @Test fun singlesAndNewMergeIntoSummary() = runTest {
        val a = sub(1, "A", "2026-01-20")
        val b = sub(2, "B", "2026-01-20")
        val c = sub(3, "C", "2026-01-15")
        val dd = sub(4, "D", "2026-01-15")
        val env = TestEnv(
            appScope(),
            listOf(a, b, c, dd),
            records = listOf(
                reminderOf(3, "2026-11-15", "2026-11-14T09:00+09:00"),
                reminderOf(4, "2026-11-15", "2026-11-14T09:00+09:00")
            ),
            now = nov14At1000
        )
        env.markReady()
        env.poster.show(
            listOf(
                ReminderPost.Single(a, d("2026-11-20")),
                ReminderPost.Single(b, d("2026-11-20"))
            ),
            env.today
        )

        env.fire()

        val summary = env.poster.active.values.single() as ReminderPost.Summary
        assertThat(summary.items.map { it.first.name }).containsExactly("A", "B", "C", "D")
    }

    @Test fun dismissedNotificationIsNotRevived() = runTest {
        val a = sub(1, "A", "2026-01-20")
        val b = sub(2, "B", "2026-01-20")
        val c = sub(3, "C", "2026-01-20")
        val dd = sub(4, "D", "2026-01-15")
        val env = TestEnv(
            appScope(),
            listOf(a, b, c, dd),
            records = listOf(reminderOf(4, "2026-11-15", "2026-11-14T09:00+09:00")),
            now = nov14At1000
        )
        env.markReady()
        val nov20 = d("2026-11-20")
        val singleA = ReminderPost.Single(a, nov20)
        env.poster.show(
            listOf(singleA, ReminderPost.Single(b, nov20), ReminderPost.Single(c, nov20)),
            env.today
        )
        env.poster.dismiss(singleA.tag)

        env.fire()

        assertThat(env.poster.active.values).containsExactly(
            ReminderPost.Single(b, nov20),
            ReminderPost.Single(c, nov20),
            ReminderPost.Single(dd, d("2026-11-15"))
        )
    }

    @Test fun notifiesWithScheduledBillingDate() = runTest {
        val workout = sub(
            1,
            "운동",
            "2026-01-01",
            cycle = BillingCycle.Days(1),
            reminder = Reminder(true, 7, LocalTime.of(9, 0))
        )
        val env = TestEnv(appScope(), listOf(workout), now = at("2026-11-16T08:00+09:00"))
        env.startup.start()
        advanceUntilIdle()
        assertThat(
            env.scheduled.snapshot.getValue(workout.id).billingDate
        ).isEqualTo(d("2026-11-23"))
        env.setNow(at("2026-11-16T10:00+09:00"))

        env.fire()

        assertThat(env.poster.posted).containsExactly(ReminderPost.Single(workout, d("2026-11-23")))
    }

    @Test fun lateAfterBillingDateIsConsumedSilently() = runTest {
        val env = TestEnv(
            appScope(),
            listOf(netflix),
            records = listOf(netflixNov),
            now = at("2026-11-16T10:00+09:00")
        )
        env.markReady()

        assertThat(env.fire()).isEqualTo(Admission.Ran(0))

        assertThat(env.poster.posted).isEmpty()
        assertThat(
            env.scheduled.snapshot.getValue(netflix.id).billingDate
        ).isEqualTo(d("2026-12-15"))
    }

    @Test fun editedOrDeletedIsConsumedSilently() = runTest {
        val edited = netflix.copy(reminder = Reminder(true, 1, LocalTime.of(9, 30)))
        val env = TestEnv(
            appScope(),
            listOf(edited),
            records = listOf(netflixNov, reminderOf(9, "2026-11-15", "2026-11-14T09:00+09:00")),
            now = nov14At1000
        )
        env.markReady()

        assertThat(env.fire()).isEqualTo(Admission.Ran(0))

        assertThat(env.poster.posted).isEmpty()
        assertThat(env.scheduled.snapshot).containsExactly(
            netflix.id,
            reminderOf(1, "2026-12-15", "2026-12-14T09:30+09:00")
        )
    }

    @Test fun cannotPostConsumesWithoutPosting() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), listOf(netflixNov), nov14At1000)
        env.markReady()
        env.poster.canPost = false

        assertThat(env.fire()).isEqualTo(Admission.Ran(0))

        assertThat(env.poster.posted).isEmpty()
        assertThat(
            env.scheduled.snapshot.getValue(netflix.id).billingDate
        ).isEqualTo(d("2026-12-15"))
    }

    @Test fun concurrentDeliveriesDoNotDuplicate() {
        val env = TestEnv(idleScope(), listOf(netflix), listOf(netflixNov), nov14At1000)
        runBlocking { env.markReady() }
        val barrier = CyclicBarrier(2)
        val results = ConcurrentLinkedQueue<Any>()
        val threads = List(2) {
            thread(isDaemon = true) {
                barrier.await(10, TimeUnit.SECONDS)
                results += runCatching { runBlocking { env.deliver() } }.getOrElse { it }
            }
        }
        threads.forEach { it.join(10_000) }

        assertThat(results).containsExactly(Admission.Ran(1), Admission.Ran(0))
        assertThat(env.poster.posted).hasSize(1)
    }

    @Test fun recordReplacedMeanwhileIsNotConsumed() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), now = nov14At0800)
        env.startup.start()
        advanceUntilIdle()
        env.setNow(nov14At1000)
        // 처리 전에 편집이 알림 시각을 바꿔, 11-15 기록이 12-15 기록으로 바뀝니다.
        val edited = netflix.copy(reminder = Reminder(true, 1, LocalTime.of(9, 30))).toDraft()
        assertThat(env.save(edited, 0)).isEqualTo(Outcome.Ok(netflix.id))
        env.refresher.refreshNow()
        val replaced = reminderOf(1, "2026-12-15", "2026-12-14T09:30+09:00")
        assertThat(env.scheduled.snapshot.getValue(netflix.id)).isEqualTo(replaced)

        env.fire()

        assertThat(env.scheduled.snapshot.getValue(netflix.id)).isEqualTo(replaced)
        assertThat(env.poster.posted).isEmpty()
    }

    /**
     * 게시 뒤 소비 전에 3번 끊깁니다. 가짜 poster는 바뀌지 않은 게시를 다시 하지 않으므로, 소비되지 않은
     * occurrence가 실제로 다시 게시되는 경로를 보려고 시도 사이에 사용자가 알림을 지웁니다.
     */
    @Test fun repeatedInterruptionAfterPostKeepsOneActiveNotification() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), listOf(netflixNov), nov14At1000)
        env.markReady()
        val tag = ReminderPost.Single(netflix, d("2026-11-15")).tag

        repeat(3) { attempt ->
            if (attempt > 0) env.poster.dismiss(tag)
            env.poster.failAfterPost = CancellationException("interrupted after post")

            val failure = runCatching { env.fire() }.exceptionOrNull()

            assertThat(failure).isInstanceOf(CancellationException::class.java)
            assertThat(env.scheduled.snapshot).containsExactly(netflix.id, netflixNov)
            // 기록이 아직 지난 채라 끝의 sync는 재시도 알람(now + 15분)을 그대로 둡니다(곧바로 다시 울리지 않음).
            assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))
            assertThat(env.poster.active).hasSize(1)
        }
        assertThat(env.poster.posted.size).isAtLeast(3)

        assertThat(env.fire()).isEqualTo(Admission.Ran(1))
        assertThat(env.poster.active).hasSize(1)
        assertThat(
            env.scheduled.snapshot.getValue(netflix.id).billingDate
        ).isEqualTo(d("2026-12-15"))
    }

    /** 처리가 매번 실패해도(취소 아님) 알람은 now로 걸리지 않고 15분 재시도 간격을 유지합니다. */
    @Test fun persistentDeliveryFailureKeepsRetryInterval() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), listOf(netflixNov), nov14At1000)
        env.markReady()

        repeat(3) { cycle ->
            val now = nov14At1000.plusSeconds(cycle * 15L * 60)
            env.setNow(now)
            env.scheduled.failNextRemoveIfMatches = IOException("db locked")

            assertThat(
                runCatching {
                    env.fire()
                }.exceptionOrNull()
            ).isInstanceOf(IOException::class.java)

            assertThat(env.alarm.nextAt).isEqualTo(now.plusSeconds(15L * 60))
            assertThat(env.scheduled.snapshot).containsExactly(netflix.id, netflixNov)
        }
        // setNext는 onAlarmFired의 재시도 알람뿐이었습니다(now로 건 적 없음).
        assertThat(env.alarm.setCount).isEqualTo(3)
    }

    /** 처리가 실패해도 끝의 sync는 실행되어 기록을 맞춥니다(사라진 구독의 기록 정리). */
    @Test fun failedDeliveryStillReconciles() = runTest {
        val orphan = reminderOf(9, "2026-11-15", "2026-11-14T09:00+09:00")
        val env = TestEnv(appScope(), emptyList(), listOf(orphan), nov14At1000)
        env.markReady()
        env.scheduled.failNextRemoveIfMatches = IOException("db locked")

        assertThat(
            runCatching {
                env.fire()
            }.exceptionOrNull()
        ).isInstanceOf(IOException::class.java)

        assertThat(env.scheduled.snapshot).isEmpty()
        assertThat(env.alarm.nextAt).isNull()
    }

    @Test fun cancelledDeliveryLeavesRetryAlarm() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), listOf(netflixNov), nov14At1000)
        env.markReady()
        env.scheduled.callDelay = 1.seconds
        val job = launch { env.fire() }
        // 기록 읽기(1초) 뒤 게시, 소비(1초 지연) 전에 취소합니다.
        advanceTimeBy(1500.milliseconds)
        assertThat(env.poster.posted).hasSize(1)
        job.cancel()
        advanceUntilIdle()

        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))
        assertThat(env.scheduled.snapshot).containsExactly(netflix.id, netflixNov)

        env.scheduled.callDelay = 0.seconds
        env.setNow(at("2026-11-14T10:15+09:00"))
        assertThat(env.fire()).isEqualTo(Admission.Ran(1))

        assertThat(
            env.scheduled.snapshot.getValue(netflix.id).billingDate
        ).isEqualTo(d("2026-12-15"))
        assertThat(env.poster.active).hasSize(1)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-12-14T09:00+09:00"))
    }

    @Test fun slowThousandDueUsesOneReadAndOneConsume() = runTest {
        val subs = (1L..1000L).map { sub(id = it, name = "S$it") }
        val records = subs.map {
            PlannedReminder(it.id, d("2026-11-15"), at("2026-11-14T09:00+09:00"))
        }
        val env = TestEnv(appScope(), subs, records, nov14At1000)
        env.markReady()
        env.repo.callDelay = 3.seconds
        env.scheduled.callDelay = 3.seconds

        // 기록 읽기 3초 + 구독 읽기 3초 + 소비 3초 > 수신기 제한 8초.
        assertThat(withTimeoutOrNull(8.seconds) { env.fire() }).isNull()

        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))
        assertThat(env.scheduled.snapshot).isEqualTo(records.associateBy { it.subscriptionId })
        assertThat(env.repo.getAllCount).isEqualTo(1)
        assertThat(env.repo.getCount).isEqualTo(0)

        env.repo.callDelay = 0.seconds
        env.scheduled.callDelay = 0.seconds
        env.setNow(at("2026-11-14T10:15+09:00"))
        assertThat(env.fire()).isEqualTo(Admission.Ran(1000))

        // 두 번째 처리: 구독 읽기 1번 + 끝의 sync 1번, 소비 1번, 구독마다 get 하지 않음.
        assertThat(env.repo.getAllCount).isEqualTo(3)
        assertThat(env.repo.getCount).isEqualTo(0)
        assertThat(env.scheduled.removeIfMatchesCount).isEqualTo(1)
        assertThat(env.scheduled.snapshot.values.map { it.billingDate }.toSet())
            .containsExactly(d("2026-12-15"))
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-12-14T09:00+09:00"))
    }

    @Test fun refreshPrunesExpiredNotifications() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.markReady()
        env.poster.show(listOf(ReminderPost.Single(netflix, env.today.minusDays(1))), env.today)

        env.refresher.refreshNow()

        assertThat(env.poster.active).isEmpty()
    }
}
