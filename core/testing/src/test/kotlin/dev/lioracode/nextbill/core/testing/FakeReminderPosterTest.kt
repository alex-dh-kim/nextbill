package dev.lioracode.nextbill.core.testing

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.tag
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeReminderPosterTest {
    private val poster = FakeReminderPoster()
    private val today = d("2026-11-14")
    private val a = ReminderPost.Single(TestSubscriptions.netflix, d("2026-11-15"))
    private val b = ReminderPost.Single(TestSubscriptions.phoneBill, d("2026-11-16"))
    private val all = setOf(
        TestSubscriptions.netflix.id,
        TestSubscriptions.phoneBill.id,
        TestSubscriptions.youtube.id
    )

    @Test
    fun dayChangeRepostsSingleSilentlyButSummaryIsUnchanged() {
        val summary = ReminderPost.Summary(
            listOf(
                TestSubscriptions.youtube to d("2026-11-20"),
                TestSubscriptions.gym to d("2026-11-27")
            )
        )
        poster.show(listOf(a, summary), today)
        poster.show(listOf(a, summary), today)
        assertThat(poster.posted).hasSize(2)

        // 내일 → 오늘: 같은 post라도 남은 날 문구가 바뀌어 조용히 다시 게시합니다(진짜와 같은 규칙).
        poster.show(listOf(a, summary), d("2026-11-15"))
        assertThat(poster.posted).containsExactly(a, summary, a).inOrder()
        assertThat(poster.silentPosts).containsExactly(a)

        // D-3 → D-2처럼 같은 문구 종류 안에서도 숫자가 바뀌면 다시 게시합니다.
        val later = ReminderPost.Single(TestSubscriptions.phoneBill, d("2026-11-20"))
        poster.show(listOf(later), d("2026-11-17"))
        poster.show(listOf(later), d("2026-11-17"))
        poster.show(listOf(later), d("2026-11-18"))
        assertThat(poster.posted.filter { it == later }).hasSize(2)
    }

    @Test
    fun pruneDropsItemsOfDeletedOrCancelledSubscriptions() {
        val summary = ReminderPost.Summary(
            listOf(
                TestSubscriptions.netflix to d("2026-11-20"),
                TestSubscriptions.youtube to d("2026-11-20")
            )
        )
        poster.show(listOf(b, summary), today)
        poster.pruneExpired(today, setOf(TestSubscriptions.youtube.id))
        assertThat(poster.active.keys).containsExactly("summary")
        assertThat((poster.active["summary"] as ReminderPost.Summary).items.map { it.first.id })
            .containsExactly(TestSubscriptions.youtube.id)
        poster.pruneExpired(today, emptySet())
        assertThat(poster.active).isEmpty()
    }

    @Test
    fun newItemsPostWithSoundUnchangedAreNotRepostedAndMissingAreCancelled() {
        poster.show(listOf(a), today)
        assertThat(poster.silentPosts).isEmpty()
        poster.show(listOf(a, b), today)
        assertThat(poster.posted).containsExactly(a, b).inOrder()
        poster.show(listOf(b), today)
        assertThat(poster.active.keys).containsExactly(b.tag)
        assertThat(poster.posted).hasSize(2)
    }

    @Test
    fun alreadyShownItemsRepostSilently() {
        val items = listOf(a, b).map { it.subscription to it.billingDate }
        poster.show(listOf(a, b), today)
        poster.show(listOf(ReminderPost.Summary(items)), today)
        assertThat(poster.silentPosts).hasSize(1)
        assertThat(poster.active.keys).containsExactly("summary")
    }

    @Test
    fun rejectsNewTagsAtMaxActive() {
        val small = FakeReminderPoster(maxActive = 1)
        small.show(listOf(a, b), today)
        assertThat(small.active.keys).containsExactly(a.tag)
        assertThat(small.rejected).containsExactly(b)
    }

    @Test
    fun pruneDropsPastSinglesAndTrimsSummaries() {
        val summary = ReminderPost.Summary(
            listOf(
                TestSubscriptions.netflix to d("2026-11-13"),
                TestSubscriptions.youtube to d("2026-11-20")
            )
        )
        poster.show(listOf(a, summary), today)
        poster.pruneExpired(d("2026-11-16"), all)
        assertThat(poster.active.keys).containsExactly("summary")
        assertThat((poster.active["summary"] as ReminderPost.Summary).items).hasSize(1)
        assertThat(poster.silentPosts).hasSize(1)
        poster.pruneExpired(d("2026-12-01"), all)
        assertThat(poster.active).isEmpty()
    }

    @Test
    fun dismissRemovesFromShown() {
        poster.show(listOf(a), today)
        poster.dismiss(a.tag)
        assertThat(poster.shown()).isEmpty()
    }

    @Test
    fun pruneGateAndCancelGatePauseCallersOnOtherThreads() = runBlocking {
        val pool = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        try {
            poster.show(listOf(a), today)
            val gate = CompletableDeferred<Unit>()
            poster.pruneGate = gate
            val job = launch(pool) { poster.pruneExpired(d("2026-12-01"), all) }
            poster.pruneEntered.await()
            assertThat(poster.active).isNotEmpty()
            gate.complete(Unit)
            job.join()
            assertThat(poster.active).isEmpty()

            val alarm = FakeReminderAlarm(nextAt = at("2026-11-14T09:00+09:00"))
            val cancelGate = CompletableDeferred<Unit>()
            alarm.cancelGate = cancelGate
            val cancelJob = launch(pool) { alarm.cancel() }
            alarm.cancelEntered.await()
            assertThat(alarm.nextAt).isNotNull()
            cancelGate.complete(Unit)
            cancelJob.join()
            assertThat(alarm.nextAt).isNull()
        } finally {
            pool.close()
        }
    }

    @Test
    fun resetEnteredAllowsReuse() = runBlocking {
        val pool = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        try {
            repeat(2) {
                val gate = CompletableDeferred<Unit>()
                poster.pruneGate = gate
                val job = launch(pool) { poster.pruneExpired(today, all) }
                poster.pruneEntered.await()
                gate.complete(Unit)
                job.join()
                poster.resetEntered()
                assertThat(poster.pruneEntered.isCompleted).isFalse()
            }
        } finally {
            pool.close()
        }
    }

    @Test
    fun alarmResetEnteredAllowsReuse() = runBlocking {
        val pool = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        try {
            val alarm = FakeReminderAlarm()
            repeat(2) {
                val gate = CompletableDeferred<Unit>()
                alarm.cancelGate = gate
                val job = launch(pool) { alarm.cancel() }
                alarm.cancelEntered.await()
                gate.complete(Unit)
                job.join()
                alarm.resetEntered()
                assertThat(alarm.cancelEntered.isCompleted).isFalse()
            }
        } finally {
            pool.close()
        }
    }

    @Test
    fun removeIfMatchesRemovesOnlyExactRows() = runTest {
        val r =
            PlannedReminder(
                TestSubscriptions.netflix.id,
                d("2026-11-15"),
                at("2026-11-14T09:00+09:00")
            )
        val other =
            PlannedReminder(TestSubscriptions.gym.id, d("2026-11-27"), at("2026-11-26T09:00+09:00"))
        val repo = FakeScheduledReminderRepository(listOf(r, other))
        val stale = r.copy(billingDate = d("2026-10-15"))
        assertThat(repo.removeIfMatches(listOf(stale, other))).isEqualTo(1)
        assertThat(repo.getAll().values).containsExactly(r)
        assertThat(repo.removeIfMatchesCount).isEqualTo(1)
    }
}
