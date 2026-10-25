package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.d
import org.junit.Test

class DueLabelTest {
    @Test fun labels() {
        val today = d("2026-12-30")

        assertThat(dueLabel(d("2026-12-30"), today)).isEqualTo(DueLabel.Today)
        assertThat(dueLabel(d("2026-12-31"), today)).isEqualTo(DueLabel.Tomorrow)
        assertThat(dueLabel(d("2027-01-04"), today)).isEqualTo(DueLabel.InDays(5))
    }
}
