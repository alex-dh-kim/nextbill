package dev.lioracode.nextbill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.settings.api.SettingsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 실제 Hilt 그래프(NextBillApplication)로 MainActivity를 띄웁니다. */
@RunWith(RobolectricTestRunner::class)
class StateRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val settingsTab = hasText("Settings") and isSelectable()

    @Test
    fun tabStacksSurviveRecreation() {
        // 시작 복구(실제 Room·DataStore)가 끝나 Ready가 되면 탭이 보입니다.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(settingsTab).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(settingsTab).performClick()
        compose.onNodeWithText(openLabel(ArchiveNavKey)).performClick()
        compose.onNodeWithText(ArchiveNavKey.toString()).assertIsDisplayed()

        compose.activityRule.scenario.recreate()

        compose.onNodeWithText(ArchiveNavKey.toString()).assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText(SettingsNavKey.toString()).assertIsDisplayed()
        compose.onNode(settingsTab).assertIsSelected()
    }
}
