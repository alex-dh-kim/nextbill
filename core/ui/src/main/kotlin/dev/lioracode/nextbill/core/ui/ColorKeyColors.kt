package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.graphics.Color
import dev.lioracode.nextbill.core.model.ColorKey

fun ColorKey.toColor(): Color = when (this) {
    ColorKey.RED -> Color(0xFFE50914)
    ColorKey.BLUE -> Color(0xFF1B64DA)
    ColorKey.GREEN -> Color(0xFF00A884)
    ColorKey.AMBER -> Color(0xFFF59E0B)
    ColorKey.VIOLET -> Color(0xFF7C3AED)
    ColorKey.PINK -> Color(0xFFEC4899)
    ColorKey.SLATE -> Color(0xFF4E5968)
    ColorKey.INK -> Color(0xFF191F28)
}

/** 아바타 배경: 키 색을 카드 위에 라이트 10%, 다크 20% 투명도로 얹습니다(시안은 옅은 색 원). */
fun ColorKey.avatarBackground(dark: Boolean): Color =
    toColor().copy(alpha = if (dark) 0.2f else 0.1f)

/**
 * 아바타 글자색. 위 배경을 카드(라이트 #FFFFFF, 다크 #1C1E22) 위에 얹은 색과의 대비가 5.1:1 이상이 되도록
 * 라이트는 어둡게, 다크는 밝게 고른 값입니다(GREEN은 시안 값 #059669에서 출발). 동적 색상처럼 카드가
 * 조금 달라져도(라이트 #F7F2FA, 다크 #211F26) 4.5:1 이상을 지킵니다.
 */
fun ColorKey.avatarLetterColor(dark: Boolean): Color = if (dark) {
    when (this) {
        ColorKey.RED -> Color(0xFFF07379)
        ColorKey.BLUE -> Color(0xFF729FE8)
        ColorKey.GREEN -> Color(0xFF38BB9F)
        ColorKey.AMBER -> Color(0xFFF59E0B)
        ColorKey.VIOLET -> Color(0xFFB089F4)
        ColorKey.PINK -> Color(0xFFF27FB8)
        ColorKey.SLATE -> Color(0xFF979DA6)
        ColorKey.INK -> Color(0xFF8E9196)
    }
} else {
    when (this) {
        ColorKey.RED -> Color(0xFFB90710)
        ColorKey.BLUE -> Color(0xFF1859C2)
        ColorKey.GREEN -> Color(0xFF046E4D)
        ColorKey.AMBER -> Color(0xFF895806)
        ColorKey.VIOLET -> Color(0xFF7235DA)
        ColorKey.PINK -> Color(0xFFA5326B)
        ColorKey.SLATE -> Color(0xFF4E5968)
        ColorKey.INK -> Color(0xFF191F28)
    }
}
