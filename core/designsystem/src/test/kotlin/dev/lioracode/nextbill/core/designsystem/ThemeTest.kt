package dev.lioracode.nextbill.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.designsystem.theme.Pretendard
import dev.lioracode.nextbill.core.designsystem.theme.toNextBillColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lightPrimaryIsBrandBlue() {
        var primary = Color.Unspecified
        rule.setContent {
            NextBillTheme(darkTheme = false) { primary = MaterialTheme.colorScheme.primary }
        }
        assertThat(primary).isEqualTo(Color(0xFF1B64DA))
    }

    @Test
    fun darkUrgentTextToken() {
        var urgent = Color.Unspecified
        rule.setContent {
            NextBillTheme(darkTheme = true) { urgent = NextBillTheme.colors.urgentText }
        }
        assertThat(urgent).isEqualTo(Color(0xFFFF4D6D))
    }

    @Test
    fun darkCanvasAndCard() {
        var canvas = Color.Unspecified
        var card = Color.Unspecified
        rule.setContent {
            NextBillTheme(darkTheme = true) {
                canvas = NextBillTheme.colors.surfaceCanvas
                card = NextBillTheme.colors.surfaceCard
            }
        }
        assertThat(canvas).isEqualTo(Color(0xFF121316))
        assertThat(card).isEqualTo(Color(0xFF1C1E22))
    }

    @Test
    fun typographyFollowsDesignTokens() {
        var display = 0.sp
        var amount = 0.sp
        var label = 0.sp
        rule.setContent {
            NextBillTheme(darkTheme = false) {
                display = MaterialTheme.typography.displaySmall.fontSize
                amount = NextBillTheme.typography.amountLarge.fontSize
                label = MaterialTheme.typography.labelSmall.fontSize
            }
        }
        assertThat(display).isEqualTo(32.sp)
        assertThat(amount).isEqualTo(28.sp)
        assertThat(label).isEqualTo(11.sp)
    }

    @Test
    fun spacingTokens() {
        var margin = 0.dp
        var xl = 0.dp
        rule.setContent {
            NextBillTheme(darkTheme = false) {
                margin = NextBillTheme.spacing.margin
                xl = NextBillTheme.spacing.xl
            }
        }
        assertThat(margin).isEqualTo(20.dp)
        assertThat(xl).isEqualTo(24.dp)
    }

    @Test
    fun unmappedTypographySlotUsesPretendard() {
        var family: FontFamily? = null
        rule.setContent {
            NextBillTheme(darkTheme = false) {
                family =
                    MaterialTheme.typography.titleSmall.fontFamily
            }
        }
        assertThat(family).isEqualTo(Pretendard)
    }

    @Test
    fun dynamicColorsDeriveFromColorScheme() {
        val scheme = lightColorScheme(
            errorContainer = Color(0xFF010203),
            outline = Color(0xFF040506)
        )
        val colors = scheme.toNextBillColors()
        assertThat(colors.urgentSurface).isEqualTo(Color(0xFF010203))
        assertThat(colors.textTertiary).isEqualTo(Color(0xFF040506))
    }
}
