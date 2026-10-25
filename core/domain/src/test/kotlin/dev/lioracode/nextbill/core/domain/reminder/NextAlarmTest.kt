package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.SEOUL
import dev.lioracode.nextbill.core.domain.billing.at
import dev.lioracode.nextbill.core.domain.billing.d
import dev.lioracode.nextbill.core.domain.billing.sub
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.SubscriptionId
import org.junit.Test

class NextAlarmTest {
    private fun record(id: Long, trigger: String) =
        PlannedReminder(SubscriptionId(id), d("2026-11-15"), at(trigger))

    @Test fun earliestTriggerIsEarliest() {
        val records = listOf(
            record(1, "2026-11-20T09:00+09:00"),
            record(2, "2026-11-14T09:00+09:00"),
            record(3, "2026-11-18T09:00+09:00")
        )

        assertThat(earliestTrigger(records)).isEqualTo(at("2026-11-14T09:00+09:00"))
    }

    @Test fun earliestTriggerKeepsPastValue() {
        // now(10:00)보다 이른 09:00 기록이 있어도 09:00 그대로 돌려줍니다.
        val records =
            listOf(record(1, "2026-11-15T09:00+09:00"), record(2, "2026-11-16T09:00+09:00"))

        assertThat(earliestTrigger(records)).isEqualTo(at("2026-11-15T09:00+09:00"))
    }

    @Test fun earliestTriggerNullWhenEmpty() = assertThat(earliestTrigger(emptyList())).isNull()

    @Test fun dueRemindersOnlyPastInOrder() {
        val now = at("2026-11-15T10:00+09:00")
        val r3 = record(3, "2026-11-15T08:00+09:00")
        val r1 = record(1, "2026-11-15T09:00+09:00")
        val r2 = record(2, "2026-11-15T08:00+09:00")
        val atNow = record(5, "2026-11-15T10:00+09:00")
        val future = record(4, "2026-11-15T10:01+09:00")

        assertThat(dueReminders(listOf(r3, future, r1, atNow, r2), now))
            .containsExactly(r2, r3, r1, atNow)
            .inOrder()
    }

    @Test fun thousandSubscriptionsNeedOneAlarm() {
        val now = at("2026-11-15T10:00+09:00")
        val subs = (1..MAX_SUBSCRIPTIONS).map { i ->
            sub(id = i.toLong(), anchor = "2026-01-%02d".format(i % 28 + 1))
        }

        val actions = reconcile(subs, emptyMap(), now, SEOUL)

        val puts = actions.filterIsInstance<ReminderRecordAction.Put>()
        assertThat(actions).hasSize(1_000)
        assertThat(puts).hasSize(1_000)
        val trigger = earliestTrigger(puts.map { it.reminder })
        // 11-16 결제분의 알림(11-15 09:00)은 이미 지났으므로 11-17 결제분의 하루 전 09:00이 가장 이릅니다.
        assertThat(trigger).isEqualTo(at("2026-11-16T09:00+09:00"))
    }
}
