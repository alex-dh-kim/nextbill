@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.TestSubscriptions.phoneBill
import dev.lioracode.nextbill.core.testing.TestSubscriptions.youtube
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

/** 관문: 실패한 가져오기·복구 실패 뒤에도 살아 있는 작업. */
class AdmissionGateTest {
    private val darkSettings = UserSettings.DEFAULT.copy(themeMode = ThemeMode.DARK)
    private val phoneOnly = BackupSnapshot(listOf(phoneBill), darkSettings)

    @Test fun saveWaitingDuringFailedImportIsRejected() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        env.prepare(phoneOnly)
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        env.settings.failNextReplace = IOException("datastore")
        env.settings.failNextReplaceTimes = 2
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()
        val saving = async { env.save(youtube.newDraft(), 0) }
        runCurrent()

        gate.complete(Unit)

        assertThat(importing.await()).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        assertThat(saving.await()).isEqualTo(Outcome.Err(SaveError.NotReady))
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.RecoveryFailed)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(listOf(phoneBill).withoutIds())
    }

    @Test fun settingsWriteWaitingDuringFailedImportIsRejected() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        env.prepare(phoneOnly)
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        env.settings.failNextReplace = IOException("datastore")
        env.settings.failNextReplaceTimes = 2
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()
        val updating = async { env.updateSettings { it.copy(useDynamicColor = true) } }
        runCurrent()

        gate.complete(Unit)

        assertThat(importing.await()).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        assertThat(updating.await()).isEqualTo(Outcome.Err(ActionError.NOT_READY))
        assertThat(env.settings.current).isEqualTo(UserSettings.DEFAULT)
    }

    @Test fun lateNotReadyCannotCancelAlarmSetByRecovery() = runBlocking {
        val env = TestEnv(idleScope(), listOf(netflix))
        env.startIntoRecoveryFailed(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        val gate = CompletableDeferred<Unit>()
        env.alarm.cancelGate = gate
        // cancel()은 스레드를 막으므로 처리는 다른 스레드에서 실행합니다.
        val delivering = async(Dispatchers.Default) { env.fire() }
        withTimeout(10.seconds) { env.alarm.cancelEntered.await() }
        // 취소는 coordinator 잠금을 쥔 채 실행 중입니다(alarmLock과 상관없는 쓰기도 기다림).
        val probe = async(Dispatchers.Default) { env.coordinator.write { } }
        val retrying = async(Dispatchers.Default) { env.startup.retryRecovery() }
        delay(100)
        assertThat(probe.isCompleted).isFalse()
        assertThat(retrying.isCompleted).isFalse()

        gate.complete(Unit)

        assertThat(withTimeout(10.seconds) { delivering.await() }).isEqualTo(Admission.NotReady)
        withTimeout(10.seconds) { retrying.await() }
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    /**
     * 처리가 NotReady를 돌려준 뒤에는 알람을 건드리지 않으므로, "잠금 해제 직후 멈춤 → 복구 → 재개"는
     * 처리를 끝까지 실행한 뒤 복구하는 것과 같습니다. 취소를 잠금 밖으로 옮긴 회귀(M5)는
     * [lateNotReadyCannotCancelAlarmSetByRecovery]의 잠금 probe가 잡습니다.
     */
    @Test fun recoveryAfterNotReadyReturnedKeepsAlarm() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        assertThat(env.fire()).isEqualTo(Admission.NotReady)
        assertThat(env.alarm.nextAt).isNull()
        val cancels = env.alarm.cancelCount

        env.startup.retryRecovery()
        val armed = env.alarm.nextAt
        assertThat(armed).isEqualTo(at("2026-11-14T09:00+09:00"))
        // 처리가 NotReady 뒤에 남긴 일(refresher 요청)이 이제 실행됩니다.
        advanceUntilIdle()

        assertThat(env.alarm.nextAt).isEqualTo(armed)
        assertThat(env.alarm.cancelCount).isEqualTo(cancels)
    }

    @Test fun cancelFailureLeavesAppliedUnknown() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        advanceUntilIdle()
        assertThat(env.alarm.setCount).isEqualTo(1)
        // 같은 데이터의 가져오기가 두 번 실패해 RecoveryFailed. 알람 기록은 그대로입니다.
        env.prepare(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        env.repo.failNextReplaceAll = IOException("disk")
        env.repo.failNextReplaceAllTimes = 2
        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        env.alarm.failNextCancel = IllegalStateException("cancel failed")

        assertThat(env.deliver()).isEqualTo(Admission.NotReady)
        assertThat(env.alarm.setCount).isEqualTo(1)

        env.startup.retryRecovery()

        // 같은 예정 시각이지만 applied가 Unknown이라 다시 겁니다.
        assertThat(env.alarm.setCount).isEqualTo(2)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    /** 취소가 실패하면 applied = Unknown: 복구 뒤 기록이 없어도 sync가 남은 알람을 다시 취소합니다. */
    @Test fun failedCancelIsRetriedWhenNothingIsScheduled() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        advanceUntilIdle()
        val armed = env.alarm.nextAt
        assertThat(armed).isNotNull()
        env.prepare(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
        env.repo.failNextReplaceAll = IOException("disk")
        env.repo.failNextReplaceAllTimes = 2
        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        env.alarm.failNextCancel = IllegalStateException("cancel failed")

        assertThat(env.deliver()).isEqualTo(Admission.NotReady)
        assertThat(env.alarm.nextAt).isEqualTo(armed)

        env.startup.retryRecovery()

        assertThat(env.scheduled.snapshot).isEmpty()
        assertThat(env.alarm.nextAt).isNull()
    }

    @Test fun pruneAndDeliverDoNotLoseNewItem() = runBlocking {
        val a = sub(1, "A", "2026-01-14")
        val b = sub(2, "B", "2026-01-15")
        val c = sub(3, "C", "2026-01-16")
        val dd = sub(4, "D", "2026-01-17")
        val e = sub(5, "E", "2026-01-16")
        val env = pruneScene(listOf(a, b, c, dd, e))
        env.poster.show(
            listOf(
                ReminderPost.Summary(
                    listOf(
                        a to d("2026-11-14"),
                        b to d("2026-11-15"),
                        c to d("2026-11-16"),
                        dd to d("2026-11-17")
                    )
                )
            ),
            env.today
        )

        runPruneThenDeliver(env)

        val summary = env.poster.active.values.single() as ReminderPost.Summary
        assertThat(summary.items.map { it.first.name }).containsExactly("B", "C", "D", "E")
        assertThat(env.scheduled.snapshot.getValue(e.id).billingDate).isEqualTo(d("2026-12-16"))
    }

    @Test fun pruneAndDeliverAcrossSingleBoundary() = runBlocking {
        val a = sub(1, "A", "2026-01-14")
        val b = sub(2, "B", "2026-01-14")
        val c = sub(3, "C", "2026-01-15")
        val dd = sub(4, "D", "2026-01-17")
        val e = sub(5, "E", "2026-01-16")
        val env = pruneScene(listOf(a, b, c, dd, e))
        env.poster.show(
            listOf(
                ReminderPost.Summary(
                    listOf(
                        a to d("2026-11-14"),
                        b to d("2026-11-14"),
                        c to d("2026-11-15"),
                        dd to d("2026-11-17")
                    )
                )
            ),
            env.today
        )

        runPruneThenDeliver(env)

        val singleC = ReminderPost.Single(c, d("2026-11-15"))
        val singleD = ReminderPost.Single(dd, d("2026-11-17"))
        val singleE = ReminderPost.Single(e, d("2026-11-16"))
        assertThat(env.poster.active.values).containsExactly(singleC, singleD, singleE)
        val silentSingles = env.poster.silentPosts.filterIsInstance<ReminderPost.Single>()
        assertThat(silentSingles).containsExactly(singleC, singleD)
        assertThat(env.poster.posted).contains(singleE)
    }

    @Test fun deliveryAfterRecoveryFailedDoesNothing() = runTest {
        val record = reminderOf(1, "2026-11-15", "2026-11-14T09:00+09:00")
        val env = TestEnv(
            appScope(),
            listOf(netflix),
            listOf(record),
            at("2026-11-14T10:00+09:00")
        )
        env.startIntoRecoveryFailed(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))

        assertThat(env.fire()).isEqualTo(Admission.NotReady)

        assertThat(env.poster.posted).isEmpty()
        assertThat(env.scheduled.snapshot).containsExactly(netflix.id, record)
        assertThat(env.alarm.nextAt).isNull()
        assertThat(env.alarm.cancelCount).isEqualTo(1)
    }

    @Test fun refreshDuringRecoveryFailedSkipsSync() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        val expired = ReminderPost.Single(netflix, env.today.minusDays(1))
        env.poster.show(listOf(expired), env.today)
        advanceUntilIdle()
        val reads = env.scheduled.getAllCount
        val dayChanges = env.dayChange.count

        env.refresher.refreshNow()

        assertThat(env.scheduled.getAllCount).isEqualTo(reads)
        assertThat(env.scheduled.snapshot).isEmpty()
        assertThat(env.alarm.setCount).isEqualTo(0)
        assertThat(env.poster.active.values).containsExactly(expired)
        assertThat(env.dayChange.count).isEqualTo(dayChanges + 1)
    }

    @Test fun deliveryBeforeStartupWaitsForRecovery() = runTest {
        val record = reminderOf(1, "2026-11-15", "2026-11-14T09:00+09:00")
        val env = TestEnv(
            appScope(),
            listOf(netflix),
            listOf(record),
            at("2026-11-14T10:00+09:00")
        )
        env.prepare(phoneOnly)
        env.staging.content = "fake-backup:1"
        val delivering = async { env.fire() }
        runCurrent()
        assertThat(delivering.isCompleted).isFalse()

        env.startup.start()

        assertThat(delivering.await()).isEqualTo(Admission.Ran(0))
        assertThat(env.poster.posted).isEmpty()
        val phoneId = env.repo.getAll().single().id
        assertThat(env.scheduled.snapshot.keys).containsExactly(phoneId)
    }

    @Test fun waitingSaveAfterSuccessfulImportIsStale() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        val snapshot = BackupSnapshot(listOf(netflix, phoneBill), UserSettings.DEFAULT)
        env.prepare(snapshot)
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()
        val saving = async { env.save(netflix.toDraft().copy(name = "Edited"), 0) }
        runCurrent()

        gate.complete(Unit)

        assertThat(importing.await()).isEqualTo(Outcome.Ok(2))
        assertThat(saving.await()).isEqualTo(Outcome.Err(SaveError.Missing))
        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(snapshot.subscriptions.withoutIds())
    }

    @Test fun writeOpenedAfterImportSucceeds() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        env.prepare(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))

        val generation = env.coordinator.generation.value
        val reopened: Subscription = env.repo.getAll().single()
        val result = env.save(reopened.toDraft().copy(name = "Edited"), generation)

        assertThat(result).isEqualTo(Outcome.Ok(reopened.id))
        assertThat(env.repo.get(reopened.id)!!.name).isEqualTo("Edited")
    }

    /** 11-15 10:00. E만 처리할 기록(11-16 결제분, 11-15 09:00)이 있습니다. */
    private suspend fun pruneScene(subs: List<Subscription>): TestEnv {
        val e = subs.last()
        val env = TestEnv(
            idleScope(),
            subs,
            listOf(reminderOf(e.id.value, "2026-11-16", "2026-11-15T09:00+09:00")),
            at("2026-11-15T10:00+09:00")
        )
        env.markReady()
        return env
    }

    /** refresher의 prune이 화면을 읽은 뒤 멈추고, 그사이 처리가 잠금을 기다립니다. */
    private suspend fun runPruneThenDeliver(env: TestEnv) = coroutineScope {
        val gate = CompletableDeferred<Unit>()
        env.poster.pruneGate = gate
        val refreshing = async(Dispatchers.Default) { env.refresher.refreshNow() }
        withTimeout(10.seconds) { env.poster.pruneEntered.await() }
        val delivering = async(Dispatchers.Default) { env.fire() }
        delay(100)
        assertThat(delivering.isCompleted).isFalse()

        gate.complete(Unit)

        withTimeout(10.seconds) { refreshing.await() }
        assertThat(withTimeout(10.seconds) { delivering.await() }).isEqualTo(Admission.Ran(1))
    }
}
