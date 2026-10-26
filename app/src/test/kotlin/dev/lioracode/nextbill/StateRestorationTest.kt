package dev.lioracode.nextbill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 실제 화면(설정·해지한 구독)의 영어 문구. 기본 로케일로 돌립니다. */
private const val ARCHIVE_ROW = "Cancelled subscriptions"
private const val ARCHIVE_NOTICE =
    "Cancelled subscriptions are left out of upcoming payments and reminders"
private const val LICENSES_ROW = "Open source licenses"

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
        compose.onNodeWithText(ARCHIVE_ROW).performScrollTo().performClick()
        compose.onNodeWithText(ARCHIVE_NOTICE).assertIsDisplayed()

        compose.activityRule.scenario.recreate()

        compose.onNodeWithText(ARCHIVE_NOTICE).assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText(LICENSES_ROW).performScrollTo().assertIsDisplayed()
        compose.onNode(settingsTab).assertIsSelected()
    }
}
