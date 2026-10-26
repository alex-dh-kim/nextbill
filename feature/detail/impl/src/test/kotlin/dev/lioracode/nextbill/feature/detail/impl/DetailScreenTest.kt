package dev.lioracode.nextbill.feature.detail.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.navigation.NavigationState
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private data object TestHome : NavKey

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class DetailScreenTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun actions(onClose: () -> Unit = { calls += "close" }) = DetailActions(
        onBack = { calls += "back" },
        onEdit = { calls += "edit" },
        onClose = onClose,
        onCancel = { calls += "cancel" },
        onRestore = { calls += "restore" },
        onDelete = { calls += "delete" },
        onOpenNotificationSettings = { calls += "settings" },
        onMessageShown = { calls += "messageShown" }
    )

    private fun show(
        state: DetailUiState,
        delivery: NotificationDelivery = NotificationDelivery.Available,
        actions: DetailActions = actions()
    ) {
        rule.setContent { NextBillTheme { DetailScreen(state, delivery, actions) } }
    }

    @Test
    fun closedPopsOnceAcrossRecompositions() {
        val key = DetailNavKey(1)
        val state = NavigationState(
            startKey = TestHome,
            topLevelStack = NavBackStack(TestHome),
            subStacks = mapOf(TestHome to NavBackStack(TestHome, key))
        )
        val navigator = Navigator(state)
        var closes = 0
        var tick by mutableIntStateOf(0)
        var current by mutableStateOf<DetailUiState>(DetailUiState.Closed)
        val actions = actions(onClose = {
            closes++
            navigator.pop(key)
        })
        rule.setContent {
            NextBillTheme {
                // tick이 바뀔 때마다 화면 전체가 다시 그려집니다.
                @Suppress("UNUSED_EXPRESSION")
                tick
                DetailScreen(current, NotificationDelivery.Available, actions)
            }
        }
        repeat(5) { rule.runOnIdle { tick++ } }
        rule.waitForIdle()

        assertThat(closes).isEqualTo(1)
        assertThat(state.subStacks.getValue(TestHome).toList()).containsExactly(TestHome)
    }

    @Test
    fun contentDoesNotClose() {
        show(netflixActive)
        rule.onNodeWithText("넷플릭스").assertExists()
        assertThat(calls).doesNotContain("close")
    }

    @Test
    fun activeShowsEverythingAndEditButton() {
        show(netflixActive)
        // 금액 큰 글씨와 월 환산, 두 곳.
        rule.onAllNodesWithText("₩17,000").assertCountEquals(2)
        rule.onNodeWithText("₩204,000").assertExists()
        rule.onNodeWithText("1일 전 오전 9:00").assertExists()
        rule.onNodeWithText("스탠다드 요금제 4K 지원").assertExists()
        rule.onNodeWithText("오늘").assertExists()
        rule.onNodeWithText("D-30").assertExists()
        rule.onNodeWithText("D-61").assertExists()
        rule.onNodeWithContentDescription("수정").performClick()
        assertThat(calls).containsExactly("edit")
    }

    @Test
    fun emptyMemoHidesMemoCard() {
        show(
            netflixActive.copy(subscription = netflixActive.subscription.copy(memo = ""))
        )
        rule.onNodeWithText("메모").assertDoesNotExist()
    }

    @Test
    fun cancelAsksFirstThenCalls() {
        show(netflixActive)
        rule.onNodeWithText("해지 처리").performScrollTo().performClick()
        assertThat(calls).isEmpty()
        rule.onNodeWithText("해지할까요?").assertExists()
        rule.onNodeWithText(
            "오늘 이후 결제와 알림에서 빠지고, 해지한 구독에서 다시 사용할 수 있어요."
        ).assertExists()
        rule.onNodeWithText("해지").performClick()
        assertThat(calls).containsExactly("cancel")
    }

    @Test
    fun deleteAsksFirstAndCanBeDismissed() {
        show(netflixActive)
        rule.onNodeWithText("삭제").performScrollTo().performClick()
        rule.onNodeWithText("지난 기록까지 모두 지워지고 되돌릴 수 없어요.").assertExists()
        rule.onNodeWithText("취소").performClick()
        assertThat(calls).isEmpty()
        rule.onNodeWithText("삭제").performScrollTo().performClick()
        rule.onNodeWithText("삭제할까요?").assertExists()
        // 대화상자의 확인 버튼은 두 번째 "삭제"입니다.
        rule.onAllNodesWithText("삭제").onLast().performClick()
        assertThat(calls).containsExactly("delete")
    }

    @Test
    fun beforeFirstBillingOffersDeleteOnly() {
        show(beforeFirstBilling)
        rule.onNodeWithText("해지 처리").assertDoesNotExist()
        rule.onNodeWithText("다시 사용").assertDoesNotExist()
        rule.onNodeWithText("아직 첫 결제 전이라 기록 없이 삭제돼요").assertExists()
        rule.onNodeWithText("삭제").assertExists()
    }

    @Test
    fun cancelledIsReadOnly() {
        show(netflixCancelled)
        rule.onNodeWithContentDescription("수정").assertDoesNotExist()
        rule.onNodeWithText("3월 2일 해지").assertExists()
        rule.onNodeWithText("다가오는 결제일").assertDoesNotExist()
        rule.onNodeWithText("해지 처리").assertDoesNotExist()
        rule.onNodeWithText("다시 사용").performScrollTo().performClick()
        assertThat(calls).containsExactly("restore")
    }

    @Test
    fun busyDisablesButtons() {
        show(netflixActive.copy(busy = true))
        rule.onNodeWithText("해지 처리").assertIsNotEnabled()
        rule.onNodeWithText("삭제").assertIsNotEnabled()
        rule.onNodeWithContentDescription("수정").assertIsNotEnabled()
    }

    @Test
    fun reminderOffAndSameDay() {
        val reminder = netflixActive.subscription.reminder
        show(
            netflixActive.copy(
                subscription = netflixActive.subscription.copy(
                    reminder = reminder.copy(enabled = false)
                )
            )
        )
        rule.onNodeWithText("꺼짐").assertExists()
    }

    @Test
    fun sameDayReminderLabel() {
        val s = netflixActive.subscription
        show(netflixActive.copy(subscription = s.copy(reminder = s.reminder.copy(daysBefore = 0))))
        rule.onNodeWithText("당일 오전 9:00").assertExists()
    }

    @Test
    fun blockedNotificationsOffersSettings() {
        show(netflixActive, NotificationDelivery.BlockedInSettings)
        rule.onNodeWithText("알림을 받을 수 없어요").assertExists()
        rule.onNodeWithText("설정에서 켜기").performClick()
        assertThat(calls).containsExactly("settings")
    }

    @Test
    fun permanentlyDeniedOffersSettings() {
        show(netflixActive, NotificationDelivery.NeedsPermission(canAsk = false))
        rule.onNodeWithText("설정에서 켜기").assertExists()
    }

    @Test
    fun askablePermissionShowsNoSettingsAction() {
        show(netflixActive, NotificationDelivery.NeedsPermission(canAsk = true))
        rule.onNodeWithText("알림을 받을 수 없어요").assertExists()
        rule.onNodeWithText("설정에서 켜기").assertDoesNotExist()
    }

    @Test
    fun reminderOffIgnoresDelivery() {
        val s = netflixActive.subscription
        show(
            netflixActive.copy(subscription = s.copy(reminder = s.reminder.copy(enabled = false))),
            NotificationDelivery.BlockedInSettings
        )
        rule.onNodeWithText("꺼짐").assertExists()
        rule.onNodeWithText("설정에서 켜기").assertDoesNotExist()
    }

    @Test
    fun messageIsShownThenReported() {
        show(netflixActive.copy(message = DetailMessage.STORAGE_FAILED))
        rule.waitForIdle()
        rule.onNodeWithText("처리하지 못했어요. 다시 시도해 주세요").assertExists()
        // 스낵바가 사라진 뒤에 알립니다.
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertThat(calls).contains("messageShown")
    }
}
