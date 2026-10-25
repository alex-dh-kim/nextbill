package dev.lioracode.nextbill.widget

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import dev.lioracode.nextbill.core.testing.usd
import org.junit.Test

class WidgetStateTest {
    private val today = d("2026-11-15")

    @Test
    fun topThreeByNextDate() {
        val state = widgetState(TestSubscriptions.all, today, CurrencyCode.KRW)
        assertThat(state.rows.map { it.name to it.label }).containsExactly(
            "넷플릭스" to DueLabel.Today,
            "통신비" to DueLabel.Tomorrow,
            "유튜브 프리미엄" to DueLabel.InDays(5)
        ).inOrder()
    }

    @Test
    fun totalsSummarized() {
        // 11월: 넷플릭스 15일 17,000 + 통신비 16일 55,000 + 유튜브 20일 14,900 + 헬스장 27일 60,000
        // = 146,900 KRW, iCloud+ 24일 USD 299. 자동차보험은 3월 결제라 제외.
        val state = widgetState(TestSubscriptions.all, today, CurrencyCode.KRW)
        assertThat(state.monthTotal.primary).isEqualTo(krw(146_900))
        assertThat(state.monthTotal.others).containsExactly(usd(299))
        assertThat(state.upcomingCount).isEqualTo(5)
    }

    @Test
    fun emptyState() {
        val state = widgetState(emptyList(), today, CurrencyCode.KRW)
        assertThat(state.rows).isEmpty()
        assertThat(state.monthTotal.primary).isEqualTo(krw(0))
        assertThat(state.monthTotal.others).isEmpty()
        assertThat(state.upcomingCount).isEqualTo(0)
    }

    @Test
    fun cancelledIsExcludedFromRowsButItsPaymentUpToCancelDateCounts() {
        val cancelled = sub(
            id = 9,
            name = "해지됨",
            anchor = "2026-01-18",
            price = krw(1_000),
            periods = listOf(ActivePeriod(d("2026-01-18"), d("2026-11-18")))
        )
        val state = widgetState(listOf(cancelled), today, CurrencyCode.KRW)
        assertThat(state.rows).isEmpty()
        assertThat(state.monthTotal.primary).isEqualTo(krw(1_000))
        assertThat(state.upcomingCount).isEqualTo(1)
    }

    @Test
    fun sameDayOrderedByNameThenId() {
        val subs = listOf(
            sub(id = 12, name = "B", anchor = "2026-01-15"),
            sub(id = 11, name = "B", anchor = "2026-01-15"),
            sub(id = 13, name = "A", anchor = "2026-01-15")
        )
        val rows = widgetState(subs, today, CurrencyCode.KRW).rows
        assertThat(rows.map { it.name }).containsExactly("A", "B", "B").inOrder()
        // id 순서는 가격으로 구분합니다.
        val byId = listOf(
            sub(id = 22, name = "B", anchor = "2026-01-15", price = krw(2)),
            sub(id = 21, name = "B", anchor = "2026-01-15", price = krw(1))
        )
        assertThat(widgetState(byId, today, CurrencyCode.KRW).rows.map { it.price })
            .containsExactly(krw(1), krw(2)).inOrder()
    }
}
