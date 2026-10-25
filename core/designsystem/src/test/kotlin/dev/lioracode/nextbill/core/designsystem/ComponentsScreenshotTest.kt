package dev.lioracode.nextbill.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbDestructiveTextButton
import dev.lioracode.nextbill.core.designsystem.component.NbListItem
import dev.lioracode.nextbill.core.designsystem.component.NbNavItem
import dev.lioracode.nextbill.core.designsystem.component.NbNavigationBar
import dev.lioracode.nextbill.core.designsystem.component.NbPill
import dev.lioracode.nextbill.core.designsystem.component.NbPrimaryButton
import dev.lioracode.nextbill.core.designsystem.component.NbSecondaryButton
import dev.lioracode.nextbill.core.designsystem.component.NbSwitch
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.component.PillStyle
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class ComponentsScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test fun pillsLight() = capture("pills_light", dark = false) { Pills() }

    @Test fun pillsDark() = capture("pills_dark", dark = true) { Pills() }

    @Test fun cardLight() = capture("card_light", dark = false) { SampleCard() }

    @Test fun cardDark() = capture("card_dark", dark = true) { SampleCard() }

    @Test fun navigationBarLight() = capture("nav_light", dark = false) { Nav() }

    @Test fun navigationBarDark() = capture("nav_dark", dark = true) { Nav() }

    @Test fun buttonsLight() = capture("buttons_light", dark = false) { Buttons() }

    @Test fun buttonsDark() = capture("buttons_dark", dark = true) { Buttons() }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            NextBillTheme(darkTheme = dark) {
                Column(
                    Modifier
                        .width(360.dp)
                        .background(NextBillTheme.colors.surfaceCanvas)
                        .padding(NextBillTheme.spacing.margin)
                ) { content() }
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}

@Composable
private fun Pills() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NbPill("D-5", style = PillStyle.Neutral)
        NbPill("오늘", style = PillStyle.Urgent)
        NbPill("내일", style = PillStyle.Urgent)
    }
}

@Composable
private fun SampleCard() {
    NbCard {
        NbListItem(
            title = "넷플릭스",
            supporting = "매월 15일",
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₩17,000",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    NbPill("오늘", style = PillStyle.Urgent)
                }
            }
        )
        NbListItem(
            title = "유튜브 프리미엄",
            supporting = "매월 20일",
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₩14,900",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    NbPill("D-5")
                }
            }
        )
    }
}

@Composable
private fun Nav() {
    NbNavigationBar(
        items = listOf(
            NbNavItem("홈", NbIcons.Home),
            NbNavItem("캘린더", NbIcons.CalendarMonth),
            NbNavItem("설정", NbIcons.Settings)
        ),
        selected = 0,
        onSelect = {}
    )
}

@Composable
private fun Buttons() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NbTopAppBar(title = "NextBill", navigationIcon = NbIcons.ArrowBack)
        NbPrimaryButton("저장", onClick = {})
        NbSecondaryButton("취소", onClick = {})
        NbDestructiveTextButton("삭제", onClick = {})
        NbSwitch(checked = true, onCheckedChange = {})
    }
}
