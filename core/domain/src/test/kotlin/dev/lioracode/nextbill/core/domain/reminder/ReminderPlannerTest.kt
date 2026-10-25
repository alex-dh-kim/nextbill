package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.SEOUL
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class ReminderPlannerTest {
    private fun reminder(daysBefore: Int, time: LocalTime = LocalTime.of(9, 0)) =
        Reminder(enabled = true, daysBefore = daysBefore, time = time)

    @Test fun oneDayBeforeAtNine() {
        val planned = sub().nextReminder(at("2026-11-10T00:00+09:00"), SEOUL)

        assertThat(planned).isEqualTo(
            PlannedReminder(SubscriptionId(1), d("2026-11-15"), at("2026-11-14T09:00+09:00"))
        )
    }

    @Test fun todayTimePassedSkipsToNextCycle() {
        val planned = sub(reminder = reminder(0)).nextReminder(at("2026-11-15T10:00+09:00"), SEOUL)

        assertThat(planned?.billingDate).isEqualTo(d("2026-12-15"))
        assertThat(planned?.triggerAt).isEqualTo(at("2026-12-15T09:00+09:00"))
    }

    @Test fun todayTimeNotYetPassed() {
        val planned = sub(reminder = reminder(0)).nextReminder(at("2026-11-15T08:59+09:00"), SEOUL)

        assertThat(planned?.billingDate).isEqualTo(d("2026-11-15"))
        assertThat(planned?.triggerAt).isEqualTo(at("2026-11-15T09:00+09:00"))
    }

    @Test fun daysBeforeLongerThanCycle() {
        val planned = sub(cycle = BillingCycle.Days(1), reminder = reminder(7))
            .nextReminder(at("2026-11-15T10:00+09:00"), SEOUL)

        assertThat(planned?.billingDate).isEqualTo(d("2026-11-23"))
        assertThat(planned?.triggerAt).isEqualTo(at("2026-11-16T09:00+09:00"))
    }

    @Test fun disabledOrCancelledReturnsNull() {
        val now = at("2026-11-10T00:00+09:00")
        val disabled = sub(reminder = Reminder.DEFAULT.copy(enabled = false))
        val cancelled = sub(periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-12-31"))))

        assertThat(disabled.nextReminder(now, SEOUL)).isNull()
        // 해지 예정일 전에 결제일(11-15)이 남아 있어도 해지된 구독은 알리지 않습니다.
        assertThat(cancelled.nextReminder(now, SEOUL)).isNull()
    }

    @Test fun pausedOccurrenceIsSkipped() {
        val paused = sub(
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-10-20")),
                ActivePeriod(d("2026-11-20"), null)
            )
        )

        val planned = paused.nextReminder(at("2026-11-10T00:00+09:00"), SEOUL)

        assertThat(planned?.billingDate).isEqualTo(d("2026-12-15"))
        assertThat(planned?.triggerAt).isEqualTo(at("2026-12-14T09:00+09:00"))
    }

    @Test fun dstGapShiftsForward() {
        val ny = ZoneId.of("America/New_York")
        val s = sub(anchor = "2027-03-14", reminder = reminder(0, LocalTime.of(2, 30)))
        val now = ZonedDateTime.of(LocalDate.of(2027, 3, 13), LocalTime.of(12, 0), ny).toInstant()

        val planned = s.nextReminder(now, ny)

        val expected = ZonedDateTime.of(d("2027-03-14"), LocalTime.of(2, 30), ny).toInstant()
        assertThat(planned?.billingDate).isEqualTo(d("2027-03-14"))
        assertThat(planned?.triggerAt).isEqualTo(expected)
        // 02:30은 없는 시각이라 한 시간 뒤(03:30 EDT)로 밀립니다.
        assertThat(expected).isEqualTo(at("2027-03-14T03:30-04:00"))
    }

    @Test fun reminderTriggerIsDaysBeforeAtTime() =
        assertThat(sub(reminder = reminder(3)).reminderTrigger(d("2026-11-15"), SEOUL))
            .isEqualTo(at("2026-11-12T09:00+09:00"))

    @Test fun stillValidOnlyWhenRecordMatchesCurrentSettings() {
        val s = sub()
        val today = d("2026-11-15")
        val record = PlannedReminder(s.id, d("2026-11-15"), at("2026-11-14T09:00+09:00"))

        assertThat(s.isStillValid(record, today, SEOUL)).isTrue()
        assertThat(
            s.isStillValid(record.copy(triggerAt = at("2026-11-14T10:00+09:00")), today, SEOUL)
        ).isFalse()
        assertThat(s.isStillValid(record, d("2026-11-16"), SEOUL)).isFalse()
        assertThat(
            s.isStillValid(
                record.copy(
                    billingDate = d("2026-11-16"),
                    triggerAt = at("2026-11-15T09:00+09:00")
                ),
                today,
                SEOUL
            )
        ).isFalse()
        assertThat(
            s.copy(reminder = s.reminder.copy(enabled = false)).isStillValid(record, today, SEOUL)
        ).isFalse()
        assertThat(
            sub(periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-12-31"))))
                .isStillValid(record, today, SEOUL)
        ).isFalse()
    }
}
