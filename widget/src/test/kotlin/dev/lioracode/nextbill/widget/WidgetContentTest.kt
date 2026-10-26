package dev.lioracode.nextbill.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ko")
class WidgetContentTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val state = widgetState(TestSubscriptions.all, d("2026-11-15"), CurrencyCode.KRW)

    private fun render(
        state: WidgetState,
        size: DpSize,
        check: GlanceAppWidgetUnitTest.() -> Unit
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { WidgetContent(state, size) }
        check()
    }

    @Test
    fun fourByTwoFitsTwoRows() = render(state, DpSize(250.dp, 110.dp)) {
        onNode(hasText("이번 달 예정")).assertExists()
        onNode(hasText("₩146,900")).assertExists()
        onNode(hasText("외 1개 통화")).assertExists()
        onNode(hasText("넷플릭스")).assertExists()
        onNode(hasText("통신비")).assertExists()
        onNode(hasText("오늘")).assertExists()
        onNode(hasText("내일")).assertExists()
        onNode(hasText("유튜브 프리미엄")).assertDoesNotExist()
        onNode(hasText("3개 결제 예정")).assertDoesNotExist()
    }

    @Test
    fun tallShowsThreeRowsAndFooter() = render(state, DpSize(250.dp, 180.dp)) {
        onNode(hasText("넷플릭스")).assertExists()
        onNode(hasText("통신비")).assertExists()
        onNode(hasText("유튜브 프리미엄")).assertExists()
        onNode(hasText("D-5")).assertExists()
        onNode(hasText("${state.upcomingCount}개 결제 예정")).assertExists()
    }

    @Test
    fun twoByTwoShowsNextOnly() = render(state, DpSize(110.dp, 110.dp)) {
        onNode(hasText("다음 결제")).assertExists()
        onNode(hasText("넷플릭스")).assertExists()
        onNode(hasText("₩17,000")).assertExists()
        onNode(hasText("오늘")).assertExists()
        onNode(hasText("통신비")).assertDoesNotExist()
        onNode(hasText("이번 달 예정")).assertDoesNotExist()
    }

    @Test
    fun emptyState() = render(
        widgetState(emptyList(), d("2026-11-15"), CurrencyCode.KRW),
        DpSize(250.dp, 110.dp)
    ) {
        onNode(hasText("등록된 구독이 없어요")).assertExists()
        onNode(hasText("다음 결제")).assertDoesNotExist()
    }

    @Test
    fun emptyStateAlsoFitsTwoByTwo() = render(
        widgetState(emptyList(), d("2026-11-15"), CurrencyCode.KRW),
        DpSize(110.dp, 110.dp)
    ) {
        onNode(hasText("등록된 구독이 없어요")).assertExists()
    }

    @Test
    fun twoRowListFitsInside110dp() {
        assertThat(listContentHeight(rows = rowsFor(DpSize(250.dp, 110.dp)), footer = false))
            .isAtMost(110.dp)
        assertThat(rowsFor(DpSize(250.dp, 110.dp))).isEqualTo(2)
    }

    @Test
    fun threeRowListWithFooterFitsInside180dp() {
        assertThat(listContentHeight(rows = rowsFor(DpSize(250.dp, 180.dp)), footer = true))
            .isAtMost(180.dp)
    }

    @Test
    fun threeKrwPaymentsGiveBriefTotalAndCount() {
        val three = widgetState(
            listOf(
                sub(1, "넷플릭스", "2026-01-15", price = krw(17_000)),
                sub(2, "통신비", "2026-01-16", price = krw(55_000)),
                sub(3, "유튜브 프리미엄", "2026-01-20", price = krw(14_400))
            ),
            d("2026-11-15"),
            CurrencyCode.KRW
        )
        render(three, DpSize(250.dp, 180.dp)) {
            onNode(hasText("₩86,400")).assertExists()
            onNode(hasText("3개 결제 예정")).assertExists()
            onNode(hasText("외 1개 통화")).assertDoesNotExist()
        }
    }
}
