package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.ui.toColor
import dev.lioracode.nextbill.feature.editor.impl.R

private val SwatchSize = 32.dp

/**
 * 색상 8개. 원은 시안대로 32dp지만 터치 영역(레이아웃)은 48dp입니다. 한 줄에 48dp × 8 = 384dp가 들어가지
 * 않는 폭(대부분의 휴대폰)에서는 4개씩 두 줄로 놓습니다.
 */
@Composable
internal fun ColorSwatches(
    selected: ColorKey,
    onSelect: (ColorKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    EditorCard(modifier = modifier, verticalSpacing = 4.dp) {
        FieldLabel(stringResource(R.string.editor_color_label))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val keys = ColorKey.entries
            val rows = if (maxWidth >= MinTouchTarget * keys.size) listOf(keys) else keys.chunked(4)
            Column {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { key ->
                            Swatch(key, key == selected, enabled, onClick = { onSelect(key) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(key: ColorKey, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val name = stringResource(key.nameRes())
    val description = if (selected) stringResource(R.string.editor_selected, name) else name
    val interaction = remember { MutableInteractionSource() }
    val ring = MaterialTheme.colorScheme.primary
    val dark = NextBillTheme.isDark
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinTouchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (enabled) {
                    onClick {
                        onClick()
                        true
                    }
                } else {
                    disabled()
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier
                            .border(2.dp, ring, CircleShape)
                            .padding(4.dp)
                    } else {
                        Modifier
                    }
                )
                .size(SwatchSize)
                .clip(CircleShape)
                .background(key.toColor())
                // 다크 카드 위에서 어두운 색(검정·회색)의 윤곽이 보이도록 옅은 테두리를 둡니다.
                .then(
                    if (dark) {
                        Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.24f),
                            CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .indication(interaction, ripple(color = Color.White))
        ) {
            if (selected) {
                Icon(
                    painterResource(R.drawable.editor_ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun ColorKey.nameRes(): Int = when (this) {
    ColorKey.RED -> R.string.editor_color_red
    ColorKey.BLUE -> R.string.editor_color_blue
    ColorKey.GREEN -> R.string.editor_color_green
    ColorKey.AMBER -> R.string.editor_color_amber
    ColorKey.VIOLET -> R.string.editor_color_violet
    ColorKey.PINK -> R.string.editor_color_pink
    ColorKey.SLATE -> R.string.editor_color_slate
    ColorKey.INK -> R.string.editor_color_ink
}
