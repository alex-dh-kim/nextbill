package dev.lioracode.nextbill.core.domain.validation

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.d
import dev.lioracode.nextbill.core.domain.billing.sub
import dev.lioracode.nextbill.core.domain.validation.FieldError.AMOUNT_BLANK
import dev.lioracode.nextbill.core.domain.validation.FieldError.AMOUNT_INVALID
import dev.lioracode.nextbill.core.domain.validation.FieldError.ANCHOR_OUT_OF_RANGE
import dev.lioracode.nextbill.core.domain.validation.FieldError.INTERVAL_INVALID
import dev.lioracode.nextbill.core.domain.validation.FieldError.MEMO_TOO_LONG
import dev.lioracode.nextbill.core.domain.validation.FieldError.NAME_BLANK
import dev.lioracode.nextbill.core.domain.validation.FieldError.NAME_TOO_LONG
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Outcome.Err
import dev.lioracode.nextbill.core.model.Outcome.Ok
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

fun draft(
    name: String = "Netflix",
    amountText: String = "17000",
    currency: CurrencyCode = CurrencyCode.KRW,
    interval: Int = 1,
    unit: CycleUnit = CycleUnit.MONTHS,
    anchorDate: LocalDate = d("2026-01-15"),
    memo: String = ""
) = SubscriptionDraft(
    name = name,
    amountText = amountText,
    currency = currency,
    interval = interval,
    unit = unit,
    anchorDate = anchorDate,
    memo = memo,
    reminder = Reminder.DEFAULT
)

private fun Outcome<Set<FieldError>, Subscription>.errors() = (this as Err).error

private fun Outcome<Set<FieldError>, Subscription>.value() = (this as Ok).value

class ValidateSubscriptionTest {
    @Test fun collectsAllErrors() {
        assertThat(draft(name = " ", amountText = "", interval = 0).validate(null).errors())
            .containsExactly(NAME_BLANK, AMOUNT_BLANK, INTERVAL_INVALID)
    }

    @Test fun nameIsTrimmedAndLengthChecked() {
        assertThat(draft(name = "  Netflix  ").validate(null).value().name).isEqualTo("Netflix")
        assertThat(draft(name = "a".repeat(30)).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(
            draft(name = "a".repeat(31)).validate(null).errors()
        ).containsExactly(NAME_TOO_LONG)
        assertThat(
            draft(name = " " + "a".repeat(30) + " ").validate(null)
        ).isInstanceOf(Ok::class.java)
    }

    @Test fun memoLengthChecked() {
        assertThat(draft(memo = "m".repeat(500)).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(
            draft(memo = "m".repeat(501)).validate(null).errors()
        ).containsExactly(MEMO_TOO_LONG)
    }

    @Test fun lengthLimitsCountCodePoints() {
        val emoji = "\uD83D\uDE00"
        assertThat(draft(name = emoji.repeat(30)).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(draft(name = emoji.repeat(31)).validate(null).errors())
            .containsExactly(NAME_TOO_LONG)
        assertThat(draft(memo = emoji.repeat(500)).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(draft(memo = emoji.repeat(501)).validate(null).errors())
            .containsExactly(MEMO_TOO_LONG)
    }

    @Test fun unsupportedCurrencyThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            draft(currency = CurrencyCode("GBP")).validate(null)
        }
    }

    @Test fun collectsMemoAndAnchorErrorsToo() {
        val errors = draft(
            name = "",
            amountText = "x",
            interval = 366,
            memo = "m".repeat(501),
            anchorDate = d("2201-01-01")
        ).validate(null).errors()
        assertThat(errors).containsExactly(
            NAME_BLANK,
            AMOUNT_INVALID,
            INTERVAL_INVALID,
            MEMO_TOO_LONG,
            ANCHOR_OUT_OF_RANGE
        )
    }

    @Test fun intervalRange() {
        assertThat(draft(interval = 1).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(draft(interval = 365).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(draft(interval = 366).validate(null).errors()).containsExactly(INTERVAL_INVALID)
        assertThat(draft(interval = -1).validate(null).errors()).containsExactly(INTERVAL_INVALID)
    }

    @Test fun amountErrorIsReported() {
        assertThat(
            draft(amountText = "1.5").validate(null).errors()
        ).containsExactly(AMOUNT_INVALID)
    }

    @Test fun buildsSubscriptionFromDraft() {
        val s = draft(
            amountText = "9.99",
            currency = CurrencyCode.USD,
            interval = 2,
            unit = CycleUnit.WEEKS
        )
            .validate(null).value()
        assertThat(s.price).isEqualTo(Money(999, CurrencyCode.USD))
        assertThat(s.cycle).isEqualTo(BillingCycle.Weeks(2))
    }

    @Test fun newDraftGetsSinglePeriodFromAnchor() {
        val s = draft(anchorDate = d("2026-03-10")).validate(null).value()
        assertThat(s.periods).containsExactly(ActivePeriod(d("2026-03-10"), null))
    }

    @Test fun editKeepsPausedPeriods() {
        val periods = listOf(
            ActivePeriod(d("2026-01-15"), d("2026-02-20")),
            ActivePeriod(d("2026-04-01"), d("2026-05-10")),
            ActivePeriod(d("2026-07-01"), null)
        )
        val existing = sub(periods = periods)
        val s = draft(anchorDate = d("2026-01-15")).validate(existing).value()
        assertThat(s.periods).isEqualTo(periods)
    }

    @Test fun editMovesFirstPeriodWithAnchor() {
        val existing = sub(
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-02-20")),
                ActivePeriod(d("2026-04-01"), null)
            )
        )
        val s = draft(anchorDate = d("2026-01-20")).validate(existing).value()
        assertThat(s.anchorDate).isEqualTo(d("2026-01-20"))
        assertThat(s.periods).containsExactly(
            ActivePeriod(d("2026-01-20"), d("2026-02-20")),
            ActivePeriod(d("2026-04-01"), null)
        ).inOrder()
    }

    @Test fun editDropsPeriodsEndedBeforeNewAnchor() {
        val existing = sub(
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-02-20")),
                ActivePeriod(d("2026-04-01"), null)
            )
        )
        val s = draft(anchorDate = d("2026-03-01")).validate(existing).value()
        assertThat(s.periods).containsExactly(ActivePeriod(d("2026-03-01"), null))
    }

    @Test fun anchorOutOfRange() {
        assertThat(draft(anchorDate = d("1899-12-31")).validate(null).errors())
            .containsExactly(ANCHOR_OUT_OF_RANGE)
        assertThat(draft(anchorDate = d("2201-01-01")).validate(null).errors())
            .containsExactly(ANCHOR_OUT_OF_RANGE)
        assertThat(draft(anchorDate = d("1900-01-01")).validate(null)).isInstanceOf(Ok::class.java)
        assertThat(draft(anchorDate = d("2200-12-31")).validate(null)).isInstanceOf(Ok::class.java)
    }

    @Test fun anchorOutOfRangeOnEditDoesNotThrow() {
        assertThat(draft(anchorDate = d("2201-01-01")).validate(sub()).errors())
            .containsExactly(ANCHOR_OUT_OF_RANGE)
    }

    @Test fun cancelledExistingSurfacesRequire() {
        val cancelled = sub(periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-02-01"))))
        assertThrows(IllegalArgumentException::class.java) { draft().validate(cancelled) }
    }

    @Test fun roundTripsThroughDraft() {
        val s = sub(price = Money(999, CurrencyCode.USD), cycle = BillingCycle.Years(2))
        assertThat(s.toDraft().amountText).isEqualTo("9.99")
        assertThat(s.toDraft().unit).isEqualTo(CycleUnit.YEARS)
        assertThat(s.toDraft().validate(s)).isEqualTo(Ok(s))
    }

    @Test fun roundTripsKrw() {
        val s = sub(price = Money(1_234_000, CurrencyCode.KRW))
        assertThat(s.toDraft().amountText).isEqualTo("1234000")
        assertThat(s.toDraft().validate(s)).isEqualTo(Ok(s))
    }
}
