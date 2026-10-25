package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.validation.CycleUnit
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.toCycle
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.ui.cycleChargeHint
import dev.lioracode.nextbill.feature.editor.impl.R
import java.time.LocalDate

private val TrackShape = RoundedCornerShape(12.dp)
private val SegmentShape = RoundedCornerShape(8.dp)
private const val MAX_INTERVAL_DIGITS = 3
private const val STACK_FONT_SCALE = 1.5f

/**
 * 결제 주기: 숫자 스테퍼(1..365, 직접 입력 가능) + 단위 세그먼트 + "매월 15일에 결제돼요" 안내.
 * 입력란이 비면 [onIntervalChange]에 0을 넘겨 검증 오류(INTERVAL_INVALID)가 되게 합니다.
 */
@Composable
internal fun CycleField(
    interval: Int,
    unit: CycleUnit,
    anchor: LocalDate,
    showError: Boolean,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (CycleUnit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = NextBillTheme.colors
    EditorCard(modifier = modifier, verticalSpacing = 12.dp) {
        FieldLabel(stringResource(R.string.editor_cycle_label))
        // 큰 글꼴에서는 "개월"이 세그먼트 칸에 들어가지 않으므로 스테퍼 아래에 전체 폭으로 놓습니다.
        if (LocalDensity.current.fontScale >= STACK_FONT_SCALE) {
            Stepper(interval, enabled, onIntervalChange)
            UnitSegments(
                unit,
                enabled,
                onUnitChange,
                Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stepper(interval, enabled, onIntervalChange)
                UnitSegments(unit, enabled, onUnitChange, Modifier.weight(1f).fillMaxHeight())
            }
        }
        val valid = interval in BillingCycle.INTERVAL_RANGE
        if (valid) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painterResource(R.drawable.editor_ic_info),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    cycleChargeHint(unit.toCycle(interval), anchor),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        } else if (showError) {
            FieldErrorText(FieldError.INTERVAL_INVALID)
        }
    }
}

@Composable
private fun Stepper(interval: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val colors = NextBillTheme.colors
    val range = BillingCycle.INTERVAL_RANGE
    val buttonColors = IconButtonDefaults.iconButtonColors(
        contentColor = colors.textSecondary,
        disabledContentColor = colors.textTertiary.copy(alpha = 0.5f)
    )
    val intervalLabel = stringResource(R.string.editor_cycle_interval)
    Row(
        modifier = Modifier
            .clip(TrackShape)
            .background(colors.surfaceCanvas)
            .border(BorderStroke(1.dp, colors.borderSubtle), TrackShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 버튼은 48dp 터치 영역(레이아웃 크기)이고, 트랙 자체가 버튼 높이를 따릅니다.
        IconButton(
            onClick = { onChange((interval - 1).coerceIn(range)) },
            enabled = enabled && interval > range.first,
            colors = buttonColors,
            modifier = Modifier.size(MinTouchTarget)
        ) {
            Icon(
                painterResource(R.drawable.editor_ic_remove),
                stringResource(R.string.editor_cycle_decrease),
                Modifier.size(18.dp)
            )
        }
        BasicTextField(
            value = if (interval == 0) "" else interval.toString(),
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS)
                onChange(digits.toIntOrNull() ?: 0)
            },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                // 세 자리(365)가 큰 글꼴에서도 잘리지 않도록 글꼴 배율만큼 넓힙니다.
                .width(36.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                .semantics { contentDescription = intervalLabel }
        )
        IconButton(
            onClick = { onChange((interval + 1).coerceIn(range)) },
            enabled = enabled && interval < range.last,
            colors = buttonColors,
            modifier = Modifier.size(MinTouchTarget)
        ) {
            Icon(
                painterResource(NbIcons.Add),
                stringResource(R.string.editor_cycle_increase),
                Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 단위 세그먼트. 각 칸의 레이아웃(터치) 높이는 48dp이고, 보이는 흰 칸은 4dp 안쪽에 그립니다.
 * 360dp 창에서 칸 폭은 약 36dp입니다(4칸 × 48dp를 넣으면 스테퍼와 한 줄에 들어가지 않음).
 */
@Composable
private fun UnitSegments(
    selected: CycleUnit,
    enabled: Boolean,
    onSelect: (CycleUnit) -> Unit,
    modifier: Modifier
) {
    val colors = NextBillTheme.colors
    Row(
        modifier = modifier
            .clip(TrackShape)
            .background(colors.surfaceCanvas)
            .border(BorderStroke(1.dp, colors.borderSubtle), TrackShape)
            .padding(horizontal = 4.dp)
            .selectableGroup()
    ) {
        CycleUnit.entries.forEach { unit ->
            val isSelected = unit == selected
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = MinTouchTarget)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        onClick = { onSelect(unit) },
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = null
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .clip(SegmentShape)
                        .background(if (isSelected) colors.surfaceCard else colors.surfaceCanvas)
                        .indication(interaction, ripple())
                )
                Text(
                    stringResource(unit.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        colors.textSecondary
                    },
                    maxLines = 1
                )
            }
        }
    }
}

private fun CycleUnit.labelRes(): Int = when (this) {
    CycleUnit.DAYS -> R.string.editor_unit_days
    CycleUnit.WEEKS -> R.string.editor_unit_weeks
    CycleUnit.MONTHS -> R.string.editor_unit_months
    CycleUnit.YEARS -> R.string.editor_unit_years
}
