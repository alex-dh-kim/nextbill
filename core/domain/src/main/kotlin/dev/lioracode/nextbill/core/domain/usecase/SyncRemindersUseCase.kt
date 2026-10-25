package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.effect.ReminderAlarm
import dev.lioracode.nextbill.core.domain.reminder.ReminderRecordAction
import dev.lioracode.nextbill.core.domain.reminder.earliestTrigger
import dev.lioracode.nextbill.core.domain.reminder.reconcile
import dev.lioracode.nextbill.core.domain.repository.ScheduledReminderRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 예약 기록(DB)과 앱 전체에 하나뿐인 OS 알람을 맞춥니다(설계 문서 ADR-3·4).
 *
 * OS에 건 알람은 메모리 [applied]로만 기억합니다. 프로세스가 새로 시작되면 Unknown이라 첫 sync는 항상
 * 다시 겁니다. 비교는 논리 예정 시각(지났어도 now로 바꾸지 않음)으로 하므로, 이미 걸어 둔 알람이 늦어져도
 * 다른 저장·갱신이 같은 알람을 다시 걸지 않습니다. 재시도 알람(Retry)이 걸린 동안 가장 이른 기록이 이미
 * 지났으면 재시도 알람을 그대로 둡니다(처리가 계속 실패해도 15분 간격으로만 다시 시도).
 */
@Singleton
class SyncRemindersUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val scheduledRepo: ScheduledReminderRepository,
    private val alarm: ReminderAlarm,
    private val clock: Clock
) {
    private sealed interface Applied {
        data object Unknown : Applied

        data object None : Applied

        data object Retry : Applied

        data class At(val trigger: Instant) : Applied
    }

    /** sync(잠금 안)와 onAlarmFired(잠금 밖)를 직렬화합니다. 안에서는 suspend하지 않습니다. */
    private val alarmLock = Any()

    private var applied: Applied = Applied.Unknown

    /**
     * coordinator 잠금 안에서만 부릅니다. ① reconcile 결과를 DB에 반영 ② OS 알람을 가장 이른 기록에 맞춤.
     * 알람 등록 예외는 안에서 잡아 다음 sync가 다시 시도하게 하고, DB 예외(취소 제외)는 던집니다.
     */
    suspend operator fun invoke() {
        val now = clock.instant()
        val scheduled = scheduledRepo.getAll()
        reconcile(repo.getAll(), scheduled, now, clock.zone).forEach { action ->
            when (action) {
                is ReminderRecordAction.Put -> scheduledRepo.put(action.reminder)
                is ReminderRecordAction.Remove -> scheduledRepo.remove(action.id)
            }
        }
        val desired = earliestTrigger(scheduledRepo.getAll().values)
        synchronized(alarmLock) {
            runCatchingNonCancellation {
                if (desired == null) {
                    if (applied != Applied.None) alarm.cancel()
                    applied = Applied.None
                } else if (applied == Applied.Retry && desired <= now) {
                    // 지난 기록이 아직 처리되지 않았습니다(처리 실패·중단). now로 다시 걸면 곧바로 울리고
                    // 실패가 계속되면 끝없이 반복되므로, 재시도 알람(15분 간격)을 그대로 둡니다.
                } else if (applied != Applied.At(desired)) {
                    alarm.setNext(maxOf(desired, now))
                    applied = Applied.At(desired)
                }
            }.onFailure { applied = Applied.Unknown }
        }
    }

    /**
     * 알람 수신 직후, 잠금 밖에서 부릅니다. 같은 OS 알람을 now + [RETRY_DELAY]로 다시 걸어 두어, 처리가
     * 잠금을 기다리다 또는 처리 중에 끊겨도 남은 기록을 이어 처리하게 합니다.
     */
    fun onAlarmFired() {
        synchronized(alarmLock) {
            runCatchingNonCancellation {
                alarm.setNext(clock.instant().plus(RETRY_DELAY))
                applied = Applied.Retry
            }.onFailure { applied = Applied.Unknown }
        }
    }

    /** coordinator 잠금 안에서만(복구 실패 확인 직후) 부릅니다. 재시도 알람이 반복되지 않게 취소합니다. */
    fun cancelRetry() {
        synchronized(alarmLock) {
            runCatchingNonCancellation {
                alarm.cancel()
                applied = Applied.None
            }.onFailure { applied = Applied.Unknown }
        }
    }

    companion object {
        val RETRY_DELAY: Duration = Duration.ofMinutes(15)
    }
}
