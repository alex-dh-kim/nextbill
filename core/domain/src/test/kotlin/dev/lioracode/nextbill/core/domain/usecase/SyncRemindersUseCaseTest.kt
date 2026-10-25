@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.TestSubscriptions.gym
import dev.lioracode.nextbill.core.testing.TestSubscriptions.icloud
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.TestSubscriptions.phoneBill
import dev.lioracode.nextbill.core.testing.TestSubscriptions.youtube
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class SyncRemindersUseCaseTest {
    /** netflix(매월 15일, 1일 전 09:00)의 11-15 결제분 기록. */
    private val netflixNov = reminderOf(1, "2026-11-15", "2026-11-14T09:00+09:00")

    @Test fun recordsActiveEnabledOnly() = runTest {
        val gymOff = gym.copy(reminder = Reminder.DEFAULT.copy(enabled = false))
        val cancelled = youtube.copy(
            periods = listOf(ActivePeriod(d("2026-01-20"), d("2026-10-01")))
        )
        val env = TestEnv(appScope(), listOf(netflix, gymOff, cancelled))

        env.sync()

        assertThat(env.scheduled.snapshot.keys).containsExactly(netflix.id)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    @Test fun thousandRemindersUseOneOsAlarm() = runTest {
        val subs = (1..1000).map {
            sub(id = it.toLong(), name = "S$it", anchor = "2026-01-%02d".format(it % 28 + 1))
        }
        val env = TestEnv(appScope(), subs)

        env.sync()

        assertThat(env.scheduled.snapshot).hasSize(1000)
        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isNotNull()
    }

    @Test fun leavesLateRecordOfOtherSubscriptionUntouched() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), now = at("2026-11-14T08:00+09:00"))
        env.startup.start()
        advanceUntilIdle()
        val aRecord = env.scheduled.snapshot.getValue(netflix.id)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))

        env.setNow(at("2026-11-14T09:20+09:00"))
        val saved = env.save(phoneBill.newDraft(), env.coordinator.generation.value)
        advanceUntilIdle()

        val newId = (saved as Outcome.Ok).value
        assertThat(env.scheduled.snapshot.getValue(netflix.id)).isEqualTo(aRecord)
        assertThat(env.scheduled.snapshot).containsKey(newId)
        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    @Test fun deletedSubscriptionRecordIsRemoved() = runTest {
        val env = TestEnv(appScope(), listOf(netflix, phoneBill))
        env.startup.start()
        advanceUntilIdle()

        assertThat(env.delete(netflix.id, 0)).isEqualTo(Outcome.Ok(Unit))
        advanceUntilIdle()

        assertThat(env.scheduled.snapshot.keys).containsExactly(phoneBill.id)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-15T09:00+09:00"))
    }

    @Test fun firstSyncAfterProcessStartAlwaysSetsAlarm() = runTest {
        // 재부팅 뒤: DB 기록은 남았고 OS 알람은 비어 있습니다.
        val env = TestEnv(appScope(), listOf(netflix), records = listOf(netflixNov))

        env.sync()

        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isEqualTo(netflixNov.triggerAt)
    }

    @Test fun unchangedEarliestDoesNotTouchAlarm() = runTest {
        val future = TestEnv(appScope(), listOf(netflix))
        future.sync()
        future.sync()
        assertThat(future.alarm.setCount).isEqualTo(1)

        val past = TestEnv(
            appScope(),
            listOf(netflix),
            records = listOf(netflixNov),
            now = at("2026-11-14T09:30+09:00")
        )
        past.sync()
        assertThat(past.alarm.nextAt).isEqualTo(at("2026-11-14T09:30+09:00"))
        past.sync()
        assertThat(past.alarm.setCount).isEqualTo(1)
    }

    @Test fun overdueAlarmIsNotRestartedByLaterSyncs() = runTest {
        val env = TestEnv(appScope(), listOf(netflix), now = at("2026-11-14T08:00+09:00"))
        env.startup.start()
        advanceUntilIdle()
        assertThat(env.alarm.setCount).isEqualTo(1)

        listOf(
            "2026-11-14T09:20+09:00" to phoneBill,
            "2026-11-14T09:40+09:00" to youtube,
            "2026-11-14T10:00+09:00" to icloud
        ).forEach { (time, other) ->
            env.setNow(at(time))
            assertThat(env.save(other.newDraft(), 0)).isInstanceOf(Outcome.Ok::class.java)
            env.refresher.refreshNow()
            advanceUntilIdle()
        }

        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    @Test fun newProcessRearmsOverdueAtNow() = runTest {
        val env = TestEnv(
            appScope(),
            listOf(netflix),
            records = listOf(netflixNov),
            now = at("2026-11-14T10:00+09:00")
        )

        env.sync()

        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:00+09:00"))
    }

    @Test fun alarmFailureIsRetriedOnNextSync() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.alarm.failNext = SecurityException("no alarm")

        env.sync()

        assertThat(env.scheduled.snapshot).containsKey(netflix.id)
        assertThat(env.alarm.setCount).isEqualTo(0)

        env.sync()

        assertThat(env.alarm.setCount).isEqualTo(1)
        assertThat(env.alarm.nextAt).isEqualTo(netflixNov.triggerAt)
    }

    /** Retry는 어떤 At과도 다르므로, 예정 시각이 그대로여도 미래면 다음 sync가 재시도 알람을 실제 시각으로 바꿉니다. */
    @Test fun retryAlarmIsReplacedByNextSyncWhenDesiredIsFuture() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.sync()
        assertThat(env.alarm.nextAt).isEqualTo(netflixNov.triggerAt)
        // 일찍 울린 알람(또는 다른 처리)이 재시도 알람을 걸었습니다.
        env.sync.onAlarmFired()
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-10T10:15+09:00"))

        env.sync()

        assertThat(env.alarm.nextAt).isEqualTo(netflixNov.triggerAt)
    }

    /** 처리되지 않은 지난 기록이 남은 동안의 저장·갱신은 재시도 알람을 now로 바꾸지 않습니다. */
    @Test fun saveDuringRetryWithOverdueRecordKeepsRetry() = runTest {
        val now = at("2026-11-14T10:00+09:00")
        val env = TestEnv(appScope(), listOf(netflix), records = listOf(netflixNov), now = now)
        env.startup.start()
        advanceUntilIdle()
        env.sync.onAlarmFired()
        val sets = env.alarm.setCount

        assertThat(env.save(phoneBill.newDraft(), 0)).isInstanceOf(Outcome.Ok::class.java)
        env.refresher.refreshNow()
        advanceUntilIdle()

        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))
        assertThat(env.alarm.setCount).isEqualTo(sets)
        assertThat(env.scheduled.snapshot.getValue(netflix.id)).isEqualTo(netflixNov)
    }

    @Test fun firedAlarmArmsRetryBeforeWaitingForLock() = runTest {
        val now = at("2026-11-14T10:00+09:00")
        val env = TestEnv(appScope(), listOf(netflix), records = listOf(netflixNov), now = now)
        env.startup.start()
        advanceUntilIdle()
        env.prepare(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()

        env.sync.onAlarmFired()
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))

        // 수신기의 8초 제한: 잠금을 기다리다 끝납니다.
        val delivery = async { withTimeoutOrNull(8.seconds) { env.deliver() } }
        advanceTimeBy(8.seconds + 1.milliseconds)
        runCurrent()
        assertThat(delivery.await()).isNull()
        assertThat(env.scheduled.snapshot).containsExactly(netflix.id, netflixNov)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-11-14T10:15+09:00"))

        gate.complete(Unit)
        assertThat(importing.await()).isEqualTo(Outcome.Ok(1))

        // 가져온 넷플릭스(새 id)의 실제 다음 시각: 12-15 결제분, 12-14 09:00.
        val newId: SubscriptionId = env.repo.getAll().single().id
        assertThat(env.scheduled.snapshot.keys).containsExactly(newId)
        assertThat(env.alarm.nextAt).isEqualTo(at("2026-12-14T09:00+09:00"))
    }
}
