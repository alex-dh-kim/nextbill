package dev.lioracode.nextbill.feature.detail.impl

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 시안 03은 한 화면에 전부 담긴 긴 화면이라 긴 창(h1150dp)으로 찍습니다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-w360dp-h1150dp-xxhdpi")
class DetailScreenScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun netflixLight() = capture("detail_netflix_light", false, netflixActive)

    @Test fun netflixDark() = capture("detail_netflix_dark", true, netflixActive)

    @Test fun cancelledLight() = capture("detail_cancelled_light", false, netflixCancelled)

    @Test fun beforeFirstBillingLight() =
        capture("detail_before_first_billing_light", false, beforeFirstBilling)

    private fun capture(name: String, dark: Boolean, state: DetailUiState) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                DetailScreen(state, NotificationDelivery.Available, DetailActions())
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
