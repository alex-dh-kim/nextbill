package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.effect.ReminderPoster
import dev.lioracode.nextbill.core.domain.reminder.dueReminders
import dev.lioracode.nextbill.core.domain.reminder.isStillValid
import dev.lioracode.nextbill.core.domain.reminder.planShown
import dev.lioracode.nextbill.core.domain.repository.ScheduledReminderRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 수신기의 처리(설계 문서 ADR-14, best effort). 부르기 전에 수신기가 `sync.onAlarmFired()`로
 * 재시도 알람을 먼저 걸어 둡니다.
 *
 * 시각이 지난 기록을 한 번에 읽고(구독 조회 1번), 아직 유효한 것만 화면 알림에 합쳐 게시한 뒤,
 * 한 트랜잭션으로 소비합니다. 게시 뒤 소비 전에 끊기면 같은 occurrence가 다음 처리 때 다시 게시될 수
 * 있습니다(같은 tag로 대체). 결제일이 지났거나 수정·삭제된 occurrence, 게시할 수 없을 때의 occurrence는
 * 게시 없이 소비합니다.
 */
@Singleton
class DeliverDueRemindersUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val scheduledRepo: ScheduledReminderRepository,
    private val poster: ReminderPoster,
    private val coordinator: DataCoordinator,
    private val sync: SyncRemindersUseCase,
    private val refresher: DerivedStateRefresher,
    private val clock: Clock
) {
    /** 게시한 occurrence 수. 복구 실패면 아무것도 소비하지 않고 [Admission.NotReady]. */
    suspend operator fun invoke(): Admission<Int> {
        // 복구 실패면 잠금 안에서 재시도 알람을 취소합니다. 잠금 밖에서 늦게 취소하면 그사이 복구가 건
        // 새 알람을 지울 수 있습니다.
        val result = coordinator.writeWhenReady(onNotReady = { sync.cancelRetry() }) {
            try {
                deliverLocked()
            } finally {
                // NonCancellable이 아닙니다. 시간 초과로 취소되면 실행되지 않을 수 있고, 그때는 재시도 알람이 남습니다.
                runCatchingNonCancellation { sync() }
            }
        }
        refresher.request()
        return result
    }

    private suspend fun deliverLocked(): Int {
        val now = clock.instant()
        val zone = clock.zone
        val today = LocalDate.ofInstant(now, zone)
        val due = dueReminders(scheduledRepo.getAll().values, now)
        if (due.isEmpty()) return 0
        val subs = repo.getAll().associateBy { it.id }
        val deliverable = due.mapNotNull { r ->
            subs[r.subscriptionId]
                ?.takeIf { it.isStillValid(r, today, zone) }
                ?.let { it to r.billingDate }
        }
        val posted = if (poster.canPost() && deliverable.isNotEmpty()) {
            poster.show(planShown(poster.shown(), deliverable, subs, today), today)
            deliverable.size
        } else {
            0
        }
        // 소비 1번(한 트랜잭션). 그사이 바뀐 기록은 남깁니다.
        scheduledRepo.removeIfMatches(due)
        return posted
    }
}
