package dev.lioracode.nextbill.core.testing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.cancelledOn
import dev.lioracode.nextbill.core.model.SubscriptionId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeSubscriptionRepositoryTest {
    private val repo = FakeSubscriptionRepository()

    @Test
    fun upsertEmitsNewActiveList() = runTest {
        repo.observeActive().test {
            assertThat(awaitItem()).isEmpty()
            val id = repo.upsert(sub(id = 0, name = "A"))
            assertThat(id).isEqualTo(SubscriptionId(1))
            assertThat(awaitItem().map { it.name }).containsExactly("A")
        }
    }

    @Test
    fun cancelledMovesFromActiveToCancelledNewestFirst() = runTest {
        val a = repo.upsert(sub(id = 0, name = "A"))
        val b = repo.upsert(sub(id = 0, name = "B"))
        repo.upsert(repo.get(a)!!.cancelledOn(d("2026-03-01")))
        repo.upsert(repo.get(b)!!.cancelledOn(d("2026-04-01")))
        repo.observeActive().test { assertThat(awaitItem()).isEmpty() }
        repo.observeCancelled().test {
            assertThat(awaitItem().map { it.name }).containsExactly("B", "A").inOrder()
        }
    }

    @Test
    fun replaceAllDropsOldAndRenumbersIds() = runTest {
        repo.upsert(sub(id = 0, name = "Old"))
        val revision = repo.replaceAll(listOf(sub(id = 99, name = "X"), sub(id = 99, name = "Y")))
        assertThat(revision).isEqualTo(1)
        val all = repo.getAll()
        assertThat(all.map { it.name }).containsExactly("X", "Y").inOrder()
        assertThat(all.map { it.id.value }.toSet()).hasSize(2)
        assertThat(all.map { it.id.value }).doesNotContain(99L)
        assertThat(all.map { it.id.value }).doesNotContain(1L)
    }

    @Test
    fun revisionStartsAtZeroAndBumps() = runTest {
        assertThat(repo.revision()).isEqualTo(0)
        assertThat(repo.bumpRevision()).isEqualTo(1)
        repo.replaceAll(emptyList())
        assertThat(repo.revision()).isEqualTo(2)
    }

    @Test
    fun failedReplaceAllKeepsDataAndRevision() = runTest {
        repo.upsert(sub(id = 0, name = "Keep"))
        repo.failNextReplaceAll = IllegalStateException("boom")
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repo.replaceAll(listOf(sub(id = 0, name = "New"))) }
        }
        assertThat(repo.getAll().map { it.name }).containsExactly("Keep")
        assertThat(repo.revision()).isEqualTo(0)
        // one-shot
        repo.replaceAll(emptyList())
        assertThat(repo.revision()).isEqualTo(1)
    }

    @Test
    fun failNextReplaceAllCanFailSeveralTimes() = runTest {
        repo.failNextReplaceAll = IllegalStateException("boom")
        repo.failNextReplaceAllTimes = 2
        repeat(2) {
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking { repo.replaceAll(emptyList()) }
            }
        }
        repo.replaceAll(emptyList())
        assertThat(repo.revision()).isEqualTo(1)
    }

    @Test
    fun replaceAllGateHoldsTheCommit() = runTest {
        repo.upsert(sub(id = 0, name = "Old"))
        val gate = CompletableDeferred<Unit>()
        repo.replaceAllGate = gate
        val job = launch { repo.replaceAll(listOf(sub(id = 0, name = "New"))) }
        runCurrent()
        assertThat(repo.getAll().map { it.name }).containsExactly("Old")
        gate.complete(Unit)
        job.join()
        assertThat(repo.getAll().map { it.name }).containsExactly("New")
    }

    @Test
    fun callDelayUsesVirtualTime() = runTest {
        repo.callDelay = 5.seconds
        val job = launch { repo.upsert(sub(id = 0)) }
        runCurrent()
        assertThat(repo.upsertCount).isEqualTo(0)
        advanceTimeBy(5.seconds)
        runCurrent()
        job.join()
        assertThat(repo.upsertCount).isEqualTo(1)
    }

    @Test
    fun gateTimeoutIgnoresVirtualTime() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.replaceAllGate = gate
        val job = launch { repo.replaceAll(listOf(sub(id = 0, name = "New"))) }
        runCurrent()
        advanceTimeBy(30.seconds)
        advanceUntilIdle()
        assertThat(job.isActive).isTrue()
        gate.complete(Unit)
        job.join()
        assertThat(repo.getAll().map { it.name }).containsExactly("New")
    }

    @Test
    fun revisionDoesNotBlockWhileReplaceAllIsGated() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.replaceAllGate = gate
        val job = launch { repo.replaceAll(emptyList()) }
        runCurrent()
        assertThat(repo.revision()).isEqualTo(0)
        gate.complete(Unit)
        job.join()
        assertThat(repo.revision()).isEqualTo(1)
    }

    @Test
    fun resetEnteredAllowsReuse() = runTest {
        repeat(2) {
            val gate = CompletableDeferred<Unit>()
            repo.replaceAllGate = gate
            val job = launch { repo.replaceAll(emptyList()) }
            repo.replaceAllEntered.await()
            gate.complete(Unit)
            job.join()
            repo.resetEntered()
            assertThat(repo.replaceAllEntered.isCompleted).isFalse()
        }
    }
}
