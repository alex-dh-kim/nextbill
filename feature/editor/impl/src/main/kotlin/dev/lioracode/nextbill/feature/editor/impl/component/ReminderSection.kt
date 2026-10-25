package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbSwitch
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.ui.daysBeforeLabel
import dev.lioracode.nextbill.core.ui.reminderTimeText
import dev.lioracode.nextbill.feature.editor.impl.R
import java.time.LocalTime

private val RowShape = RoundedCornerShape(12.dp)

/**
 * 결제 알림 카드. [showUnavailable]이면 알림 의도는 켜져 있지만 실제로 받을 수 없다는 안내를 보여 주고,
 * 누르면 [onUnavailableClick](권한 요청 또는 앱 알림 설정)을 부릅니다.
 */
@Composable
internal fun ReminderSection(
    reminder: Reminder,
    showUnavailable: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDaysBeforeChange: (Int) -> Unit,
    onTimeClick: () -> Unit,
    onUnavailableClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = NextBillTheme.colors
    EditorCard(modifier = modifier, verticalSpacing = 12.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .toggleable(
                    value = reminder.enabled,
                    enabled = enabled,
                    onValueChange = onEnabledChange,
                    role = Role.Switch
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            ) {
                Icon(
                    painterResource(R.drawable.editor_ic_notifications_active),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.editor_reminder_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.editor_reminder_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            NbSwitch(checked = reminder.enabled, onCheckedChange = null, enabled = enabled)
        }
        if (reminder.enabled) {
            if (showUnavailable) UnavailableNotice(enabled, onUnavailableClick)
            HorizontalDivider(color = colors.borderSubtle)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FieldLabel(stringResource(R.string.editor_reminder_days_label))
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Reminder.DAYS_BEFORE_OPTIONS.forEach { days ->
                        OptionChip(
                            text = daysBeforeLabel(days),
                            selected = reminder.daysBefore == days,
                            onClick = { onDaysBeforeChange(days) },
                            selectedStyle = OptionChipStyle.Tinted,
                            fill = true,
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            TimeRow(reminder.time, enabled, onTimeClick)
        }
    }
}

@Composable
private fun UnavailableNotice(enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(RowShape)
            .background(NextBillTheme.colors.urgentSurface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            stringResource(R.string.editor_reminder_unavailable),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = NextBillTheme.colors.urgentText
        )
    }
}

@Composable
private fun TimeRow(time: LocalTime, enabled: Boolean, onClick: () -> Unit) {
    val colors = NextBillTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(RowShape)
            .background(colors.surfaceCanvas.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, colors.borderSubtle), RowShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.editor_reminder_time_label),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            reminderTimeText(time),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )
        Icon(
            painterResource(NbIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.padding(start = 4.dp).size(18.dp)
        )
    }
}
