package dev.lioracode.nextbill.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.ColorKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-w360dp-h640dp-xxhdpi")
class CoreUiScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test fun avatarsLight() = capture("avatars_light", false) { Avatars() }

    @Test fun avatarsDark() = capture("avatars_dark", true) { Avatars() }

    @Test fun dueChipsLight() = capture("due_chips_light", false) { Chips() }

    @Test fun dueChipsDark() = capture("due_chips_dark", true) { Chips() }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                Column(
                    Modifier
                        .width(360.dp)
                        .background(NextBillTheme.colors.surfaceCanvas)
                        .padding(NextBillTheme.spacing.margin)
                ) { content() }
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}

@Composable
private fun Avatars() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorKey.entries.take(4).forEach { LetterAvatar("넷플릭스", it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorKey.entries.drop(4).forEach { LetterAvatar("icloud", it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LetterAvatar("넷플릭스", ColorKey.RED, desaturated = true)
        }
    }
}

@Composable
private fun Chips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DueChip(DueLabel.Today)
        DueChip(DueLabel.Tomorrow)
        DueChip(DueLabel.InDays(5))
    }
}
