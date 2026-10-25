package dev.lioracode.nextbill.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.ColorKey
import java.util.Locale

/**
 * 이름의 첫 글자. trim 후 첫 code point(서로게이트 쌍은 통째로)이며 글자면 대문자로 바꿉니다.
 * ZWJ 이모지 시퀀스는 첫 code point만 취합니다. 빈 이름은 "?".
 */
internal fun avatarLetter(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val first = String(Character.toChars(trimmed.codePointAt(0)))
    return if (first.codePointAt(
            0
        ).let(Character::isLetter)
    ) {
        first.uppercase(Locale.ROOT)
    } else {
        first
    }
}

/**
 * 장식 요소입니다(접근성 이름은 옆의 텍스트가 가짐). [desaturated]는 보관된 구독용으로
 * 보관함 시안의 중립 회색 원(배경 badgeNeutralBg, 글자 textSecondary, 1dp borderSubtle)으로 그립니다.
 */
@Composable
fun LetterAvatar(
    name: String,
    colorKey: ColorKey,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    desaturated: Boolean = false
) {
    val colors = NextBillTheme.colors
    val dark = NextBillTheme.isDark
    val background = if (desaturated) colors.badgeNeutralBg else colorKey.avatarBackground(dark)
    val letterColor = if (desaturated) colors.textSecondary else colorKey.avatarLetterColor(dark)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clearAndSetSemantics {}
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(
                if (desaturated) {
                    Modifier.border(
                        1.dp,
                        colors.borderSubtle,
                        CircleShape
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = avatarLetter(name),
            color = letterColor,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = avatarFontSize(size))
        )
    }
}

private fun avatarFontSize(size: Dp): TextUnit = (size.value * 0.4f).sp
