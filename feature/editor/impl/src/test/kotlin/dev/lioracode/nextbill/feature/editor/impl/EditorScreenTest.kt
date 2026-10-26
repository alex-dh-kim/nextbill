package dev.lioracode.nextbill.feature.editor.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.navigation.NavigationState
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private data object TestHome : NavKey

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class EditorScreenTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun actions(onClose: () -> Unit = { calls += "close" }) = EditorActions(
        onSave = { calls += "save" },
        onClose = onClose,
        onRequestPermission = { calls += "request" },
        onPermissionLater = { calls += "later" },
        onOpenNotificationSettings = { calls += "settings" },
        onMessageShown = { calls += "messageShown" },
        onDraftChange = { transform ->
            calls +=
                "draft:" + transform(newWithAmountError.draft).category
        }
    )

    private fun show(
        state: EditorUiState?,
        delivery: NotificationDelivery = NotificationDelivery.Available,
        requestedBefore: Boolean = false,
        actions: EditorActions = actions()
    ) {
        rule.setContent {
            NextBillTheme { EditorScreen(state, delivery, requestedBefore, actions) }
        }
    }

    @Test
    fun savedPopsOnceAcrossRecompositions() {
        val key = EditorNavKey()
        val state = NavigationState(
            startKey = TestHome,
            topLevelStack = NavBackStack(TestHome),
            subStacks = mapOf(TestHome to NavBackStack(TestHome, key))
        )
        val navigator = Navigator(state)
        var closes = 0
        val saved = newWithAmountError.copy(phase = EditorPhase.Saved(SubscriptionId(1)))
        var tick by mutableIntStateOf(0)
        var current by mutableStateOf(saved)
        val actions = actions(onClose = {
            closes++
            navigator.pop(key)
        })
        rule.setContent {
            NextBillTheme {
                // tick이 바뀔 때마다 화면 전체가 다시 그려집니다. 같은 Saved 상태의 새 인스턴스도 넘깁니다.
                EditorScreen(current, NotificationDelivery.Available, tick % 2 == 0, actions)
            }
        }
        repeat(5) {
            rule.runOnIdle {
                tick++
                current = saved.copy()
            }
        }
        rule.waitForIdle()

        assertThat(closes).isEqualTo(1)
        assertThat(state.subStacks.getValue(TestHome).toList()).containsExactly(TestHome)
    }

    @Test
    fun missingCloses() {
        show(editingExisting.copy(phase = EditorPhase.Missing))
        rule.waitForIdle()
        assertThat(calls).containsExactly("close")
    }

    @Test
    fun editingDoesNotClose() {
        show(editingExisting)
        rule.onNodeWithText("구독 수정").assertExists()
        assertThat(calls).doesNotContain("close")
    }

    @Test
    fun errorsHiddenUntilSaveAttempted() {
        show(newWithAmountError.copy(showErrors = false))
        rule.onNodeWithText("금액을 입력해 주세요").assertDoesNotExist()
    }

    @Test
    fun errorsShownAfterSaveAttempted() {
        show(newWithAmountError)
        rule.onNodeWithText("금액을 입력해 주세요").assertExists()
    }

    @Test
    fun saveButtonCallsSaveAndIsDisabledWhileSaving() {
        show(newWithAmountError)
        rule.onNodeWithText("저장").performClick()
        assertThat(calls).containsExactly("save")
    }

    @Test
    fun saveButtonDisabledWhileSaving() {
        show(newWithAmountError.copy(phase = EditorPhase.Saving))
        rule.onNodeWithText("저장").assertIsNotEnabled()
    }

    @Test
    fun swatchDescriptionsIncludeSelection() {
        show(newWithAmountError)
        rule.onNodeWithContentDescription("빨강, 선택됨").assertExists()
        rule.onNodeWithContentDescription("파랑").assertExists()
    }

    @Test
    fun chipsAndSwatchesHave48dpLayoutSize() {
        show(newWithAmountError)
        rule.onNodeWithText("음악").performScrollTo()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("영상").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        for (name in listOf("빨강, 선택됨", "파랑", "검정")) {
            rule.onNodeWithContentDescription(name).performScrollTo()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        rule.onNodeWithText("당일").performScrollTo()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        for (name in listOf("감소", "증가")) {
            rule.onNodeWithContentDescription(name).performScrollTo()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        // 단위 세그먼트는 높이만 48dp입니다(360dp에서 4칸 × 48dp 폭은 스테퍼와 한 줄에 들어가지 않음).
        for (unit in listOf("일", "주", "개월", "년")) {
            rule.onNodeWithText(unit).performScrollTo().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun controlsDisabledWhileSaving() {
        show(newWithAmountError.copy(phase = EditorPhase.Saving))
        rule.onNodeWithText("음악").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("개월").assertIsNotEnabled()
        rule.onNodeWithContentDescription("증가").assertIsNotEnabled()
        rule.onNodeWithContentDescription("파랑").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("이름").assertIsNotEnabled()
        rule.onNodeWithText("결제 알림").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun tappingCategoryUpdatesDraft() {
        show(newWithAmountError)
        rule.onNodeWithText("클라우드").performScrollTo().performClick()
        assertThat(calls).containsExactly("draft:" + Category.CLOUD)
    }

    @Test
    fun noUnavailableNoticeBeforeFirstSystemRequest() {
        show(
            newWithAmountError,
            NotificationDelivery.NeedsPermission(canAsk = true),
            requestedBefore = false
        )
        rule.onNodeWithText("알림을 받을 수 없어요 · 설정에서 켜기").assertDoesNotExist()
    }

    @Test
    fun unavailableNoticeRequestsAgainWhenSystemStillAsks() {
        show(
            newWithAmountError,
            NotificationDelivery.NeedsPermission(canAsk = true),
            requestedBefore = true
        )
        rule.onNodeWithText("알림을 받을 수 없어요 · 설정에서 켜기").performScrollTo().performClick()
        assertThat(calls).containsExactly("request")
    }

    @Test
    fun unavailableNoticeOpensSettingsWhenBlocked() {
        show(newWithAmountError, NotificationDelivery.BlockedInSettings)
        rule.onNodeWithText("알림을 받을 수 없어요 · 설정에서 켜기").performScrollTo().performClick()
        assertThat(calls).containsExactly("settings")
    }

    @Test
    fun unavailableNoticeOpensSettingsWhenSystemWontAsk() {
        show(newWithAmountError, NotificationDelivery.NeedsPermission(canAsk = false))
        rule.onNodeWithText("알림을 받을 수 없어요 · 설정에서 켜기").performScrollTo().performClick()
        assertThat(calls).containsExactly("settings")
    }

    @Test
    fun noUnavailableNoticeWhenReminderOff() {
        val off = newWithAmountError.copy(
            draft = newWithAmountError.draft.copy(
                reminder = newWithAmountError.draft.reminder.copy(enabled = false)
            )
        )
        show(off, NotificationDelivery.BlockedInSettings)
        rule.onNodeWithText("알림을 받을 수 없어요 · 설정에서 켜기").assertDoesNotExist()
    }

    @Test
    fun permissionSheetLaterAndAllow() {
        show(newWithAmountError.copy(phase = EditorPhase.AskingPermission(saveAfter = true)))
        rule.onNodeWithText("결제 전에 알려드릴게요").assertExists()
        rule.onNodeWithText("결제 1일 전 오전 9:00에 잊지 않게 알려드려요").assertExists()

        rule.onNodeWithText("알림 허용하기").performClick()
        assertThat(calls).containsExactly("request")

        rule.onNodeWithText("나중에").performClick()
        rule.waitForIdle()
        assertThat(calls).containsExactly("request", "later").inOrder()
    }

    @Test
    fun messageShownThenCleared() {
        show(newWithAmountError.copy(message = EditorMessage.STORAGE_FAILED))
        rule.onNodeWithText("저장하지 못했어요. 다시 시도해 주세요").assertExists()
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertThat(calls).contains("messageShown")
    }

    @Test
    fun loadingShowsOnlyTopBar() {
        show(null)
        rule.onNodeWithText("구독 추가").assertExists()
        rule.onNodeWithText("이름").assertDoesNotExist()
    }
}
