package dev.lioracode.nextbill.feature.settings.impl

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.navigation.NavigationState
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.settings.api.LicensesNavKey
import dev.lioracode.nextbill.feature.settings.api.SettingsNavKey
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private data object TestHome : NavKey

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko-w360dp-h800dp")
class SettingsScreenTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val navState = NavigationState(
        startKey = TestHome,
        topLevelStack = NavBackStack(TestHome, SettingsNavKey),
        subStacks = mapOf(
            TestHome to NavBackStack(TestHome),
            SettingsNavKey to NavBackStack(SettingsNavKey)
        )
    )
    private val navigator = Navigator(navState)

    private fun actions() = SettingsActions(
        onDefaultReminder = { calls += "reminder ${it.daysBefore} ${it.time}" },
        onTheme = { calls += "theme $it" },
        onDynamicColor = { calls += "dynamic $it" },
        onCurrency = { calls += "currency ${it.value}" },
        onNotificationStatus = { calls += "notification" },
        onExport = { calls += "export" },
        onImport = { calls += "import" },
        onConfirmImport = { calls += "confirm" },
        onDismissImport = { calls += "dismiss" },
        onOpenArchive = { navigator.navigate(ArchiveNavKey) },
        onOpenLicenses = { navigator.navigate(LicensesNavKey) },
        onMessageShown = { calls += "messageShown" }
    )

    private fun show(
        state: SettingsUiState = settingsReady,
        delivery: NotificationDelivery = NotificationDelivery.Available,
        importPending: Boolean = false,
        dynamicColorSupported: Boolean = true
    ) {
        rule.setContent {
            NextBillTheme {
                SettingsScreen(state, delivery, dynamicColorSupported, importPending, actions())
            }
        }
    }

    private fun click(text: String) {
        rule.onNode(hasText(text) and hasClickAction(), useUnmergedTree = false)
            .performScrollTo()
            .performClick()
    }

    private val settingsStack get() = navState.subStacks.getValue(SettingsNavKey).toList()

    @Test
    fun showsDesignValues() {
        show()
        rule.onNodeWithText("1일 전").assertExists()
        rule.onNodeWithText("오전 9:00").assertExists()
        rule.onNodeWithText("허용됨").assertExists()
        rule.onNodeWithText("시스템 설정").assertExists()
        rule.onNodeWithText("KRW (₩)").assertExists()
        rule.onNodeWithText("현재 데이터가 모두 바뀌어요").assertExists()
        rule.onNodeWithText("저장 위치를 클라우드 앱으로 고르면 그곳에 저장돼요").assertExists()
        rule.onNodeWithText("1.0.0").performScrollTo().assertExists()
        rule.onNodeWithText("모든 데이터는 이 기기에만 저장돼요").performScrollTo().assertExists()
    }

    @Test
    fun archiveIsPushedOnSettingsTab() {
        show()
        rule.onNodeWithText("해지한 구독").performScrollTo().performClick()
        assertThat(settingsStack).containsExactly(SettingsNavKey, ArchiveNavKey).inOrder()
    }

    @Test
    fun licensesArePushedOnSettingsTab() {
        show()
        rule.onNodeWithText("오픈소스 라이선스").performScrollTo().performClick()
        assertThat(settingsStack).containsExactly(SettingsNavKey, LicensesNavKey).inOrder()
    }

    @Test
    fun daysBeforeDialogPicksOption() {
        show()
        rule.onNodeWithText("기본 알림 시점").performClick()
        rule.onNodeWithText("3일 전").performClick()
        assertThat(calls).containsExactly("reminder 3 09:00")
        rule.onNodeWithText("3일 전").assertDoesNotExist()
    }

    @Test
    fun themeAndCurrencyDialogs() {
        show()
        rule.onNodeWithText("테마 설정").performClick()
        rule.onNodeWithText("다크").performClick()
        rule.onNodeWithText("기본 통화").performClick()
        rule.onNodeWithText("USD ($)").performClick()
        assertThat(calls).containsExactly("theme ${ThemeMode.DARK}", "currency USD").inOrder()
    }

    @Test
    fun dynamicColorToggles() {
        show()
        rule.onNodeWithText("동적 색상 사용").performClick()
        assertThat(calls).containsExactly("dynamic false")
    }

    @Test
    fun dynamicColorHiddenBelowSdk31() {
        show(dynamicColorSupported = false)
        rule.onNodeWithText("동적 색상 사용").assertDoesNotExist()
    }

    @Test
    fun unavailableDeliveryAndTap() {
        show(delivery = NotificationDelivery.BlockedInSettings)
        rule.onNodeWithText("받을 수 없어요").assertExists()
        rule.onNodeWithText("알림 권한 상태").performClick()
        assertThat(calls).containsExactly("notification")
    }

    @Test
    fun backupRowsDisabledWhileRunning() {
        show(settingsReady.copy(backup = BackupStatus.IMPORTING))
        rule.onNode(hasText("백업 파일 내보내기") and hasClickAction()).assertIsNotEnabled()
        rule.onNode(hasText("백업 파일 가져오기") and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithText("백업 파일 내보내기").performClick()
        assertThat(calls).isEmpty()
    }

    @Test
    fun exportAndImportOpenPickers() {
        show()
        click("백업 파일 내보내기")
        click("백업 파일 가져오기")
        assertThat(calls).containsExactly("export", "import").inOrder()
    }

    @Test
    fun importAsksForConfirmation() {
        show(importPending = true)
        rule.onNodeWithText("백업 파일을 가져올까요? 현재 데이터가 모두 바뀌어요.").assertExists()
        rule.onNodeWithText("가져오기").performClick()
        rule.onNodeWithText("취소").performClick()
        assertThat(calls).containsExactly("confirm", "dismiss").inOrder()
    }

    @Test
    fun messageShownOnceAfterSnackbar() {
        show(settingsReady.copy(message = SettingsMessage.Imported(3)))
        rule.waitForIdle()
        rule.onNodeWithText("3개 구독을 가져왔어요").assertExists()
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertThat(calls).containsExactly("messageShown")
    }

    @Test
    fun timeDialogKeepsDaysBefore() {
        show(
            settingsReady.copy(
                settings = settingsReady.settings.copy(
                    defaultReminder = Reminder(true, 7, LocalTime.of(9, 0)),
                    defaultCurrency = CurrencyCode.KRW
                )
            )
        )
        rule.onNodeWithText("기본 알림 시각").performClick()
        rule.onNodeWithText("확인").performClick()
        assertThat(calls).containsExactly("reminder 7 09:00")
    }
}
