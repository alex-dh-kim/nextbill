package dev.lioracode.nextbill.feature.editor.impl

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.MAX_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.MIN_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import org.junit.Test

class EditorLogicTest {
    @Test
    fun datePickerMillisRoundTripInUtc() {
        for (date in listOf(MIN_ANCHOR_DATE, d("2026-03-29"), d("2024-02-29"), MAX_ANCHOR_DATE)) {
            assertThat(date.toUtcMillis().toUtcDate()).isEqualTo(date)
        }
        assertThat(d("1970-01-02").toUtcMillis()).isEqualTo(86_400_000L)
    }

    @Test
    fun unavailableNoticeRules() {
        val canAsk = NotificationDelivery.NeedsPermission(canAsk = true)
        val cannotAsk = NotificationDelivery.NeedsPermission(canAsk = false)
        assertThat(showUnavailable(NotificationDelivery.Available, true)).isFalse()
        assertThat(showUnavailable(canAsk, false)).isFalse()
        assertThat(showUnavailable(canAsk, true)).isTrue()
        assertThat(showUnavailable(cannotAsk, false)).isTrue()
        assertThat(showUnavailable(NotificationDelivery.BlockedInSettings, false)).isTrue()
    }

    @Test
    fun unreadableSavedDraftIsIgnored() {
        assertThat(DraftSaver.decode("not json")).isNull()
        val valid = DraftSaver.encode(
            SubscriptionDraft(
                currency = CurrencyCode.KRW,
                anchorDate = d("2026-11-15"),
                reminder = Reminder.DEFAULT
            )
        )
        assertThat(DraftSaver.decode(valid.replace("MONTHS", "FORTNIGHTS"))).isNull()
        assertThat(DraftSaver.decode(valid.replace("2026-11-15", "2026-13-40"))).isNull()
    }
}
