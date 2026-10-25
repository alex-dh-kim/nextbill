package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.ColorKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// clearAndSetSemantics only prunes the merged tree (what TalkBack reads); the unmerged tree keeps the Text.
@RunWith(RobolectricTestRunner::class)
class LetterAvatarSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test fun letterIsNotExposedToAccessibility() {
        rule.setContent { NextBillTheme { LetterAvatar("넷플릭스", ColorKey.RED) } }
        rule.onAllNodes(hasText("넷")).assertCountEquals(0)
    }

    @Test fun desaturatedLetterIsNotExposedEither() {
        rule.setContent { NextBillTheme { LetterAvatar("넷플릭스", ColorKey.RED, desaturated = true) } }
        rule.onAllNodes(hasText("넷")).assertCountEquals(0)
    }
}
