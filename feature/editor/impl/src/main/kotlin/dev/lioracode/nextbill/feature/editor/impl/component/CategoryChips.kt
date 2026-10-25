package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.ui.categoryLabel
import dev.lioracode.nextbill.feature.editor.impl.R

/** 최소 터치 영역. 보이는 칩은 더 작아도 레이아웃(터치) 크기는 이 값 이상입니다(설계 문서 §9.2). */
internal val MinTouchTarget = 48.dp
private val ChipShape = RoundedCornerShape(12.dp)

@Composable
internal fun CategoryChips(
    selected: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    EditorCard(modifier = modifier, verticalSpacing = 4.dp) {
        FieldLabel(stringResource(R.string.editor_category_label))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { category ->
                OptionChip(
                    text = categoryLabel(category),
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    selectedStyle = OptionChipStyle.Filled,
                    enabled = enabled
                )
            }
        }
    }
}

internal enum class OptionChipStyle { Filled, Tinted }

/**
 * 선택 칩. 바깥 상자가 48dp 이상의 터치 영역(레이아웃 크기)이고, 눌림 효과는 안쪽의 보이는 칩에만 그립니다.
 * [Filled]: primary 배경 + 흰 글자(카테고리). [Tinted]: 옅은 primary 배경 + primary 글자(알림 시점).
 */
@Composable
internal fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedStyle: OptionChipStyle,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    enabled: Boolean = true
) {
    val colors = NextBillTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val (background, content) = when {
        !selected -> colors.surfaceCanvas to colors.badgeNeutralText
        selectedStyle == OptionChipStyle.Filled -> primary to MaterialTheme.colorScheme.onPrimary
        else -> primary.copy(alpha = 0.12f) to primary
    }
    val border = if (selected && selectedStyle == OptionChipStyle.Tinted) {
        BorderStroke(1.dp, primary.copy(alpha = 0.2f))
    } else {
        null
    }
    Box(
        modifier = modifier
            .widthIn(min = MinTouchTarget)
            .heightIn(min = MinTouchTarget)
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .clip(ChipShape)
                .background(background)
                .then(if (border != null) Modifier.border(border, ChipShape) else Modifier)
                .indication(interaction, ripple())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
