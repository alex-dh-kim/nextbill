@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class RunCatchingTest {
    @Test fun wrapsSuccess() {
        assertThat(runCatchingNonCancellation { 3 }.getOrNull()).isEqualTo(3)
    }

    @Test fun wrapsOrdinaryFailure() {
        val e = IOException("x")
        assertThat(runCatchingNonCancellation { throw e }.exceptionOrNull()).isSameInstanceAs(e)
    }

    @Test fun rethrowsCancellation() {
        assertThrows(CancellationException::class.java) {
            runCatchingNonCancellation { throw CancellationException("stop") }
        }
    }
}

class DataCoordinatorTest {
    @Test fun startsRecoveringAndRejectsWrites() = runTest {
        val c = DataCoordinator()
        var ran = false

        assertThat(c.state.value).isEqualTo(StartupState.Recovering)
        assertThat(c.write { ran = true }).isEqualTo(Admission.NotReady)
        assertThat(ran).isFalse()
    }

    @Test fun writeChecksStateAfterAcquiringLock() = runTest {
        val c = DataCoordinator()
        c.recovery { setState(StartupState.Ready) }
        val gate = CompletableDeferred<Unit>()
        launch {
            c.recovery {
                gate.await()
                setState(StartupState.RecoveryFailed)
            }
        }
        runCurrent()
        var ran = false
        val w = async { c.write { ran = true } }
        runCurrent()

        gate.complete(Unit)

        assertThat(w.await()).isEqualTo(Admission.NotReady)
        assertThat(ran).isFalse()
    }

    @Test fun staleGenerationIsRejected() = runTest {
        val c = DataCoordinator()
        c.recovery {
            setGeneration(4)
            setState(StartupState.Ready)
        }

        assertThat(c.write(expectedGeneration = 3) { 1 }).isEqualTo(Admission.Stale)
        assertThat(c.write(expectedGeneration = 4) { 1 }).isEqualTo(Admission.Ran(1))
        assertThat(c.write { 2 }).isEqualTo(Admission.Ran(2))
        assertThat(c.generation.value).isEqualTo(4)
    }

    @Test fun writeWhenReadyRunsOnNotReadyOnlyAfterRecoveryFailed() = runTest {
        val c = DataCoordinator()
        var notReady = 0
        val w = async { c.writeWhenReady(onNotReady = { notReady++ }) { "ran" } }
        runCurrent()
        assertThat(w.isCompleted).isFalse()

        c.recovery { setState(StartupState.RecoveryFailed) }

        assertThat(w.await()).isEqualTo(Admission.NotReady)
        assertThat(notReady).isEqualTo(1)
    }

    /** P9: 잠금을 얻었는데 Recovering이면(retryRecovery의 두 블록 사이) 기다렸다 다시 검사합니다. */
    @Test fun writeWhenReadyWaitsAcrossRetryRecoveryWindow() = runTest {
        val c = DataCoordinator()
        c.recovery { setState(StartupState.RecoveryFailed) }
        val holder = CompletableDeferred<Unit>()
        launch { c.recovery { holder.await() } }
        runCurrent()
        // retryRecovery 모양: 첫 블록이 Recovering, 잠금을 놓았다가 두 번째 블록이 Ready.
        launch {
            c.recovery { setState(StartupState.Recovering) }
            c.recovery { setState(StartupState.Ready) }
        }
        runCurrent()
        var notReady = false
        // RecoveryFailed라서 상태 대기를 지나 잠금 대기열에서 retry의 첫 블록 바로 뒤에 섭니다.
        val w = async { c.writeWhenReady(onNotReady = { notReady = true }) { "ran" } }
        runCurrent()

        holder.complete(Unit)
        advanceUntilIdle()

        assertThat(w.await()).isEqualTo(Admission.Ran("ran"))
        assertThat(notReady).isFalse()
    }

    /** P9를 실제 retryRecovery·처리로: 두 블록 사이에 잠금을 얻은 처리는 NotReady가 아니라 기다립니다. */
    @Test fun deliveryInsideRetryRecoveryWindowWaitsInsteadOfNotReady() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        val holder = CompletableDeferred<Unit>()
        launch { env.coordinator.recovery { holder.await() } }
        runCurrent()
        val retry = launch { env.startup.retryRecovery() }
        runCurrent()
        val delivery = async { env.deliver() }
        runCurrent()

        holder.complete(Unit)
        retry.join()

        assertThat(delivery.await()).isEqualTo(Admission.Ran(0))
        assertThat(env.alarm.cancelCount).isEqualTo(0)
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
    }
}
