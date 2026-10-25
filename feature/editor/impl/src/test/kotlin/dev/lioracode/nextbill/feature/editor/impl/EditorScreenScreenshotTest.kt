package dev.lioracode.nextbill.feature.editor.impl

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.ui.NotificationDelivery
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
class EditorScreenScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun newWithAmountErrorLight() = capture(
        "editor_new_error_light",
        false,
        newWithAmountError
    )

    @Test fun newWithAmountErrorDark() = capture("editor_new_error_dark", true, newWithAmountError)

    /** 폼 전체(알림·메모 카드까지)가 한 장에 보이도록 긴 창으로 찍습니다. */
    @Test
    @Config(qualifiers = "ko-w360dp-h1400dp-xxhdpi")
    fun editLight() = capture("editor_edit_light", false, editingExisting)

    @Test
    fun newFontScale200Light() {
        RuntimeEnvironment.setFontScale(2f)
        capture("editor_new_font200_light", false, newWithAmountError)
    }

    /** 시트는 별도 창(dialog)에 그려지므로 화면 전체를 찍습니다. */
    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun permissionSheetLight() {
        show(false, newWithAmountError.copy(phase = EditorPhase.AskingPermission(saveAfter = true)))
        rule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/editor_permission_sheet_light.png")
    }

    private fun capture(name: String, dark: Boolean, state: EditorUiState) {
        show(dark, state)
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun show(dark: Boolean, state: EditorUiState) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                EditorScreen(state, NotificationDelivery.Available, false, EditorActions())
            }
        }
    }
}
