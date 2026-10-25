package dev.lioracode.nextbill.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.domain.billing.nextBillingDate
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.usd
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-w360dp-h800dp-xxhdpi")
class HomeScreenScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val today = d("2026-11-15")

    private val ready = HomeUiState.Ready(
        month = YearMonth.of(2026, 11),
        due = AmountSummary(krw(86_400), listOf(usd(1_998))),
        fixedCost = AmountSummary(krw(72_300), emptyList()),
        upcoming = TestSubscriptions.all.map { s ->
            val next = checkNotNull(s.nextBillingDate(today))
            UpcomingItem(
                s.id,
                s.name,
                s.colorKey,
                s.cycle,
                s.anchorDate,
                s.price,
                next,
                dueLabel(next, today)
            )
        }.sortedBy { it.nextDate }
    )

    private val empty = HomeUiState.Ready(
        month = YearMonth.of(2026, 11),
        due = AmountSummary(krw(0), emptyList()),
        fixedCost = AmountSummary(krw(0), emptyList()),
        upcoming = emptyList()
    )

    @Test fun readyLight() = capture("home_ready_light", false, ready)

    @Test fun readyDark() = capture("home_ready_dark", true, ready)

    @Test fun emptyLight() = capture("home_empty_light", false, empty)

    @Test fun readyFontScale200Light() {
        RuntimeEnvironment.setFontScale(2f)
        capture("home_ready_font200_light", false, ready)
    }

    private fun capture(name: String, dark: Boolean, state: HomeUiState) {
        rule.setContent { Screen(dark, state) }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Screen(dark: Boolean, state: HomeUiState) {
        NextBillTheme(darkTheme = dark) {
            Box(Modifier.fillMaxSize().background(NextBillTheme.colors.surfaceCanvas)) {
                HomeScreen(state = state, onAddClick = {}, onItemClick = {})
            }
        }
    }
}
