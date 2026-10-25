package dev.lioracode.nextbill.feature.archive.impl

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
class ArchiveScreenScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun listLight() = capture("archive_list_light", archiveReady)

    @Test fun emptyLight() = capture("archive_empty_light", ArchiveUiState.Ready(emptyList(), null))

    private fun capture(name: String, state: ArchiveUiState) {
        rule.setContent { NextBillTheme { ArchiveScreen(state, ArchiveActions()) } }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
