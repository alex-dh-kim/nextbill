package dev.lioracode.nextbill.feature.settings.impl

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.settings.impl.licenses.LicensesScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 시안 05는 탭 바를 뺀 높이로도 길어 긴 창(h1000dp)으로 찍습니다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-w360dp-h1000dp-xxhdpi")
class SettingsScreenScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun settingsLight() = capture("settings_light", dark = false)

    @Test fun settingsDark() = capture("settings_dark", dark = true)

    @Test
    fun licensesLight() {
        val catalog = realLicenseCatalog()
        rule.setContent { NextBillTheme { LicensesScreen(catalog, onBack = {}) } }
        rule.onRoot().captureRoboImage("src/test/screenshots/licenses_light.png")
    }

    private fun capture(name: String, dark: Boolean) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                SettingsScreen(
                    state = settingsReady,
                    delivery = NotificationDelivery.Available,
                    dynamicColorSupported = true,
                    importPending = false,
                    actions = SettingsActions()
                )
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
