package dev.lioracode.nextbill

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NextBillAppTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var navigator: Navigator

    private fun setApp(dataGeneration: () -> Long = { 0L }) {
        compose.setContent {
            val state = rememberNextBillNavigationState()
            navigator = remember(state) { Navigator(state) }
            NextBillTheme {
                NextBillApp(navigator, dataGeneration(), entries = ::placeholderEntries)
            }
        }
    }

    @Test
    fun showsThreeTabsAtStart() {
        setApp()

        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Calendar").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun bottomBarHiddenOnPushedScreen() {
        setApp()

        compose.onNodeWithText("Settings").performClick()
        compose.runOnIdle { navigator.navigate(ArchiveNavKey) }

        compose.onNodeWithText(ArchiveNavKey.toString()).assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()

        compose.runOnIdle { navigator.goBack() }

        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()
    }

    @Test
    fun dataGenerationChangeResetsStacksToRoots() {
        val generation = mutableLongStateOf(0L)
        setApp { generation.longValue }
        compose.runOnIdle { navigator.navigate(DetailNavKey(1)) }
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()

        compose.runOnIdle { generation.longValue = 1L }

        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()
        compose.runOnIdle {
            assertThat(navigator.state.subStacks.values.map { it.size }).containsExactly(1, 1, 1)
        }
    }
}
