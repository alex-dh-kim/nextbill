package dev.lioracode.nextbill.feature.home.impl

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class HomeScreenAccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun addButtonExposesItsLabel() {
        val netflix = TestSubscriptions.netflix
        val state = HomeUiState.Ready(
            month = YearMonth.of(2026, 11),
            due = AmountSummary(krw(17_000), emptyList()),
            fixedCost = AmountSummary(krw(17_000), emptyList()),
            upcoming = listOf(
                UpcomingItem(
                    netflix.id,
                    netflix.name,
                    netflix.colorKey,
                    netflix.cycle,
                    netflix.anchorDate,
                    netflix.price,
                    d("2026-11-15"),
                    DueLabel.Today
                )
            )
        )
        rule.setContent {
            NextBillTheme { HomeScreen(state = state, onAddClick = {}, onItemClick = {}) }
        }

        // 접근성 트리(병합)에서 버튼 이름으로 찾을 수 있어야 TalkBack이 "구독 추가, 버튼"으로 읽습니다.
        rule.onNode(hasContentDescription("구독 추가") and hasClickAction()).assertExists()
    }
}
