package dev.lioracode.nextbill.feature.archive.impl

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.navigation.NavigationState
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private data object TestHome : NavKey

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class ArchiveScreenTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun actions(onOpenDetail: (SubscriptionId) -> Unit = { calls += "open ${it.value}" }) =
        ArchiveActions(
            onBack = { calls += "back" },
            onOpenDetail = onOpenDetail,
            onRestore = { calls += "restore ${it.value}" },
            onDelete = { calls += "delete ${it.value}" },
            onMessageShown = { calls += "messageShown" }
        )

    private fun show(state: ArchiveUiState, actions: ArchiveActions = actions()) {
        rule.setContent { NextBillTheme { ArchiveScreen(state, actions) } }
    }

    @Test
    fun rowOpensDetail() {
        val navState = NavigationState(
            startKey = TestHome,
            topLevelStack = NavBackStack(TestHome),
            subStacks = mapOf(TestHome to NavBackStack(TestHome, ArchiveNavKey))
        )
        val navigator = Navigator(navState)
        show(
            archiveReady,
            actions(onOpenDetail = { navigator.navigate(DetailNavKey(it.value)) })
        )
        rule.onNodeWithText("왓챠").performClick()
        assertThat(navState.subStacks.getValue(TestHome).toList())
            .containsExactly(TestHome, ArchiveNavKey, DetailNavKey(2))
            .inOrder()
    }

    @Test
    fun showsRowsWithInfoAndNotice() {
        show(archiveReady)
        rule.onNodeWithText("해지한 구독").assertExists()
        rule.onNodeWithText("해지한 구독은 앞으로의 결제와 알림에서 빠져요").assertExists()
        rule.onNodeWithText("₩12,900 · 매월 12일").assertExists()
        rule.onNodeWithText("5월 20일 해지").assertExists()
        rule.onAllNodesWithText("3월 2일 해지").assertCountEquals(2)
    }

    @Test
    fun restoreFromMenu() {
        show(archiveReady)
        rule.onNodeWithContentDescription("왓챠 더보기").performClick()
        rule.onNodeWithText("다시 사용").performClick()
        assertThat(calls).containsExactly("restore 2")
    }

    @Test
    fun deleteAsksFirstAndCanBeDismissed() {
        show(archiveReady)
        rule.onNodeWithContentDescription("왓챠 더보기").performClick()
        rule.onNodeWithText("삭제").performClick()
        assertThat(calls).isEmpty()
        rule.onNodeWithText("삭제할까요?").assertExists()
        rule.onNodeWithText("취소").performClick()
        assertThat(calls).isEmpty()

        rule.onNodeWithContentDescription("왓챠 더보기").performClick()
        rule.onNodeWithText("삭제").performClick()
        rule.onNodeWithText("삭제").performClick()
        assertThat(calls).containsExactly("delete 2")
    }

    @Test
    fun emptyState() {
        show(ArchiveUiState.Ready(emptyList(), null))
        rule.onNodeWithText("해지한 구독이 없어요").assertExists()
        rule.onAllNodesWithContentDescription("왓챠 더보기").assertCountEquals(0)
    }

    @Test
    fun messageShownOnceAfterSnackbar() {
        show(ArchiveUiState.Ready(emptyList(), ArchiveMessage.RESTORED))
        rule.waitForIdle()
        rule.onNodeWithText("다시 사용해요").assertExists()
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertThat(calls).containsExactly("messageShown")
    }

    @Test
    fun backCallsBack() {
        show(archiveReady)
        rule.onNodeWithContentDescription("뒤로 가기").performClick()
        assertThat(calls).containsExactly("back")
    }
}
