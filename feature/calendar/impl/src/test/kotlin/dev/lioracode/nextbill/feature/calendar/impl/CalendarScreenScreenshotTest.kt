package dev.lioracode.nextbill.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-w360dp-h800dp-xxhdpi")
class CalendarScreenScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun withPaymentsLight() = capture(
        "calendar_payments_light",
        false,
        novState("2026-11-15")
    )

    @Test fun withPaymentsDark() = capture("calendar_payments_dark", true, novState("2026-11-15"))

    @Test fun emptyDayLight() = capture(
        "calendar_empty_light",
        false,
        novState("2026-11-17", false)
    )

    @Test fun emptyDayDark() = capture("calendar_empty_dark", true, novState("2026-11-17", false))

    private fun capture(name: String, dark: Boolean, state: CalendarUiState) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(NextBillTheme.colors.surfaceCanvas)) {
                    CalendarScreen(state, {}, {}, {}, {}, {})
                }
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
