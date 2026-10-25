package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.SEOUL
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.time.LocalTime
import org.junit.Test

class ReminderReconcilerTest {
    private val now = at("2026-11-15T10:00+09:00")

    private fun apply(
        scheduled: Map<SubscriptionId, PlannedReminder>,
        actions: List<ReminderRecordAction>
    ): Map<SubscriptionId, PlannedReminder> {
        val result = scheduled.toMutableMap()
        actions.forEach {
            when (it) {
                is ReminderRecordAction.Put -> result[it.reminder.subscriptionId] = it.reminder
                is ReminderRecordAction.Remove -> result.remove(it.id)
            }
        }
        return result
    }

    @Test fun keepsLateButValidRecord() {
        val a = sub(id = 1, reminder = Reminder(true, 0, LocalTime.of(9, 0)))
        val late = PlannedReminder(a.id, d("2026-11-15"), at("2026-11-15T09:00+09:00"))

        val actions = reconcile(listOf(a), mapOf(a.id to late), now, SEOUL)

        assertThat(actions).isEmpty()
    }

    @Test fun keepsUnchangedRecord() {
        val s = sub()
        val existing = PlannedReminder(s.id, d("2026-12-15"), at("2026-12-14T09:00+09:00"))
        assertThat(s.nextReminder(now, SEOUL)).isEqualTo(existing)

        assertThat(reconcile(listOf(s), mapOf(s.id to existing), now, SEOUL)).isEmpty()
    }

    @Test fun replacesWhenReminderSettingsChanged() {
        val s = sub(reminder = Reminder(true, 3, LocalTime.of(9, 0)))
        val old = PlannedReminder(s.id, d("2026-12-15"), at("2026-12-14T09:00+09:00"))

        val actions = reconcile(listOf(s), mapOf(s.id to old), now, SEOUL)

        assertThat(actions).containsExactly(
            ReminderRecordAction.Put(
                PlannedReminder(s.id, d("2026-12-15"), at("2026-12-12T09:00+09:00"))
            )
        )
    }

    @Test fun removesWhenDisabledOrCancelledOrDeleted() {
        val disabled = sub(id = 1, reminder = Reminder.DEFAULT.copy(enabled = false))
        val cancelled =
            sub(id = 2, periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-01"))))
        val deletedId = SubscriptionId(3)
        fun record(id: SubscriptionId) =
            PlannedReminder(id, d("2026-12-15"), at("2026-12-14T09:00+09:00"))
        val scheduled = listOf(disabled.id, cancelled.id, deletedId).associateWith { record(it) }

        val actions = reconcile(listOf(disabled, cancelled), scheduled, now, SEOUL)

        assertThat(actions).containsExactly(
            ReminderRecordAction.Remove(disabled.id),
            ReminderRecordAction.Remove(cancelled.id),
            ReminderRecordAction.Remove(deletedId)
        ).inOrder()
    }

    @Test fun removesLateRecordOfCancelledSubscription() {
        // 시각이 지났어도 무효가 된 기록은 보존하지 않습니다.
        val cancelled = sub(periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-15"))))
        val late = PlannedReminder(cancelled.id, d("2026-11-15"), at("2026-11-14T09:00+09:00"))

        assertThat(reconcile(listOf(cancelled), mapOf(cancelled.id to late), now, SEOUL))
            .containsExactly(ReminderRecordAction.Remove(cancelled.id))
    }

    @Test fun putsNewSubscription() {
        val s = sub()

        assertThat(reconcile(listOf(s), emptyMap(), now, SEOUL)).containsExactly(
            ReminderRecordAction.Put(
                PlannedReminder(s.id, d("2026-12-15"), at("2026-12-14T09:00+09:00"))
            )
        )
    }

    @Test fun idempotent() {
        val subs = listOf(
            sub(id = 1),
            sub(id = 2, reminder = Reminder(true, 0, LocalTime.of(9, 0))),
            sub(id = 3, reminder = Reminder(true, 7, LocalTime.of(20, 0))),
            sub(id = 4, reminder = Reminder.DEFAULT.copy(enabled = false)),
            sub(id = 5, periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-01"))))
        )
        val scheduled = mapOf(
            SubscriptionId(2) to
                PlannedReminder(SubscriptionId(2), d("2026-11-15"), at("2026-11-15T09:00+09:00")),
            SubscriptionId(3) to
                PlannedReminder(SubscriptionId(3), d("2026-12-15"), at("2026-12-14T09:00+09:00")),
            SubscriptionId(4) to
                PlannedReminder(SubscriptionId(4), d("2026-12-15"), at("2026-12-14T09:00+09:00")),
            SubscriptionId(9) to
                PlannedReminder(SubscriptionId(9), d("2026-12-15"), at("2026-12-14T09:00+09:00"))
        )

        val first = reconcile(subs, scheduled, now, SEOUL)
        assertThat(first).isNotEmpty()
        val applied = apply(scheduled, first)

        assertThat(reconcile(subs, applied, now, SEOUL)).isEmpty()
        assertThat(applied.keys).containsExactly(
            SubscriptionId(1),
            SubscriptionId(2),
            SubscriptionId(3)
        )
    }
}
