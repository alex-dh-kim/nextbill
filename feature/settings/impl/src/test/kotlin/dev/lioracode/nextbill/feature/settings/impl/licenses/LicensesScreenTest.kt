package dev.lioracode.nextbill.feature.settings.impl.licenses

import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.feature.settings.impl.realLicenseCatalog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class LicensesScreenTest {
    @get:Rule val rule = createComposeRule()

    private var backs = 0

    private fun show() {
        rule.setContent {
            NextBillTheme { LicensesScreen(realLicenseCatalog(), onBack = { backs++ }) }
        }
    }

    @Test
    fun entryOpensFullLicenseText() {
        show()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Pretendard"))
        rule.onNodeWithText("Pretendard").performClick()
        rule.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).assertExists()
        rule.onNodeWithText("닫기").performClick()
        rule.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).assertDoesNotExist()
    }

    @Test
    fun bundleShowsArtifactCount() {
        show()
        rule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("AndroidX and other transitive dependencies"))
        rule.onNodeWithText("아티팩트", substring = true).assertExists()
    }

    @Test
    fun backCallsBack() {
        show()
        rule.onNodeWithContentDescription("뒤로 가기").performClick()
        assertThat(backs).isEqualTo(1)
    }
}
