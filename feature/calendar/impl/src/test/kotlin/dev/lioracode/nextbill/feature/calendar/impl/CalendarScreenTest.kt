package dev.lioracode.nextbill.feature.calendar.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.d
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class CalendarScreenTest {
    @get:Rule val rule = createComposeRule()

    private val selected = mutableListOf<LocalDate>()
    private val opened = mutableListOf<SubscriptionId>()

    private fun show(state: CalendarUiState = novState("2026-11-15")) {
        selected.clear()
        rule.setContent {
            NextBillTheme {
                Box(Modifier.fillMaxSize()) {
                    CalendarScreen(state, {}, {}, {}, { selected += it }, { opened += it })
                }
            }
        }
    }

    @Test fun cellDescriptionsIncludeCountAndSelection() {
        show()
        rule.onNodeWithContentDescription("11월 15일, 결제 2건, 선택됨").assertExists()
        rule.onNodeWithContentDescription("11월 16일, 결제 1건").assertExists()
        rule.onNodeWithContentDescription("11월 17일").assertExists()
    }

    @Test fun selectedTitleAndPaymentsShown() {
        show()
        rule.onNodeWithText("11월 15일 (일)").assertExists()
        rule.onNodeWithText("넷플릭스").assertExists()
        rule.onNodeWithText("7건 · ₩86,400 · $19.98").assertExists()
    }

    @Test fun emptyDayShowsMessage() {
        show(novState("2026-11-17", false))
        rule.onNodeWithText("이 날은 결제가 없어요").assertExists()
    }

    @Test fun tappingPaymentOpensDetail() {
        show()
        rule.onNodeWithText("통신비").performClick()
        assertThat(opened).containsExactly(SubscriptionId(2))
    }

    @Test fun tappingCellSelectsDate() {
        show()
        rule.onNodeWithContentDescription("11월 16일, 결제 1건").performClick()
        assertThat(selected).containsExactly(d("2026-11-16"))
    }

    @Test fun cellsAre48dpAt360() {
        show()
        rule.onNodeWithContentDescription("11월 17일")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = "ko-w320dp-h640dp")
    fun cellsAreNarrowerThan48dpAt320() {
        show()
        val bounds = rule.onNodeWithContentDescription("11월 17일").getUnclippedBoundsInRoot()
        assertThat(bounds.right.value - bounds.left.value).isLessThan(48f)
    }

    @Test
    @Config(qualifiers = "ko-w320dp-h640dp")
    fun narrowWindowCellsDoNotOverlapAndCenterTapSelects() {
        show()
        val first = rule.onNodeWithContentDescription("11월 16일, 결제 1건").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithContentDescription("11월 17일").getUnclippedBoundsInRoot()
        assertThat(first.right.value).isAtMost(second.left.value + 0.01f)
        for (day in listOf(17, 18, 19)) {
            rule.onNodeWithContentDescription("11월 ${day}일").performClick()
        }
        assertThat(
            selected
        ).containsExactly(d("2026-11-17"), d("2026-11-18"), d("2026-11-19")).inOrder()
    }
}
