package dev.lioracode.nextbill.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import dev.lioracode.nextbill.core.designsystem.theme.NextBillColors
import dev.lioracode.nextbill.core.designsystem.theme.NextBillPalette
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.ui.avatarBackground
import dev.lioracode.nextbill.core.ui.avatarLetterColor

/** Glance 테마. 앱 테마와 같은 색 값을 씁니다(컴포넌트는 재사용하지 않고 값만 가져옵니다). */
internal val WidgetColorProviders = ColorProviders(
    light = NextBillPalette.scheme(dark = false),
    dark = NextBillPalette.scheme(dark = true)
)

private fun token(select: (NextBillColors) -> Color): androidx.glance.unit.ColorProvider =
    ColorProvider(
        day = select(NextBillPalette.colors(dark = false)),
        night = select(NextBillPalette.colors(dark = true))
    )

/** 시안 토큰 이름의 라이트/다크 쌍. 위젯은 시스템 다크 모드를 따릅니다. */
internal object WidgetColors {
    val card = token { it.surfaceCard }
    val textPrimary = token { it.textPrimary }
    val textSecondary = token { it.textSecondary }
    val border = token { it.borderSubtle }
    val urgentSurface = token { it.urgentSurface }
    val urgentText = token { it.urgentText }
    val neutralSurface = token { it.badgeNeutralBg }
    val neutralText = token { it.badgeNeutralText }
    val brand = ColorProvider(
        day = NextBillPalette.scheme(dark = false).primary,
        night = NextBillPalette.scheme(dark = true).primary
    )
    val onBrand = ColorProvider(day = Color.White, night = Color.White)

    fun avatarBackground(key: ColorKey) =
        ColorProvider(day = key.avatarBackground(false), night = key.avatarBackground(true))

    fun avatarLetter(key: ColorKey) =
        ColorProvider(day = key.avatarLetterColor(false), night = key.avatarLetterColor(true))
}
