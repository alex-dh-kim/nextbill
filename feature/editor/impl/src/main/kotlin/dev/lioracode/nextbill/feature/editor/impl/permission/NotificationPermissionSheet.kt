package dev.lioracode.nextbill.feature.editor.impl.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbPrimaryButton
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.ui.reminderTimeText
import dev.lioracode.nextbill.feature.editor.impl.R
import kotlinx.coroutines.launch

/**
 * 알림 권한 안내 시트(시안 07, 설계 문서 ADR-5). "알림 허용하기"는 OS 권한 대화상자를 띄우고([onAllow]),
 * "나중에"와 시트를 아래로 내려 닫기는 모두 [onLater]입니다(요청 이력을 남기지 않음).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationPermissionSheet(
    reminder: Reminder,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = NextBillTheme.colors.surfaceCard,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 24.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    ) {
        PermissionSheetContent(
            reminder = reminder,
            onAllow = onAllow,
            onLater = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onLater() }
            }
        )
    }
}

@Composable
internal fun PermissionSheetContent(reminder: Reminder, onAllow: () -> Unit, onLater: () -> Unit) {
    val colors = NextBillTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(primary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                painterResource(R.drawable.editor_ic_notifications_active_filled),
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            ),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceCanvas, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Benefit(R.drawable.editor_ic_schedule_filled, timingText(reminder))
            Benefit(
                R.drawable.editor_ic_verified_user_filled,
                stringResource(R.string.permission_no_marketing)
            )
        }
        Spacer(Modifier.height(28.dp))
        NbPrimaryButton(
            text = stringResource(R.string.permission_allow),
            onClick = onAllow,
            trailingIcon = R.drawable.editor_ic_check,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                stringResource(R.string.permission_later),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun Benefit(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = NextBillTheme.colors.badgeNeutralText
        )
    }
}

/** "결제 1일 전 오전 9:00에 잊지 않게 알려드려요". 지금 입력한 알림 설정을 그대로 씁니다. */
@Composable
private fun timingText(reminder: Reminder): String {
    val whenText = if (reminder.daysBefore == 0) {
        stringResource(R.string.permission_timing_same_day)
    } else {
        pluralStringResource(
            R.plurals.permission_timing_days_before,
            reminder.daysBefore,
            reminder.daysBefore
        )
    }
    return stringResource(R.string.permission_timing, whenText, reminderTimeText(reminder.time))
}
