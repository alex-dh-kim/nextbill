package dev.lioracode.nextbill.feature.detail.impl

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbDestructiveTextButton
import dev.lioracode.nextbill.core.designsystem.component.NbPill
import dev.lioracode.nextbill.core.designsystem.component.NbPrimaryButton
import dev.lioracode.nextbill.core.designsystem.component.NbSecondaryButton
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.ui.DueChip
import dev.lioracode.nextbill.core.ui.LetterAvatar
import dev.lioracode.nextbill.core.ui.MoneyText
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.core.ui.categoryLabel
import dev.lioracode.nextbill.core.ui.cycleText
import dev.lioracode.nextbill.core.ui.daysBeforeLabel
import dev.lioracode.nextbill.core.ui.monthDayText
import dev.lioracode.nextbill.core.ui.reminderTimeText
import dev.lioracode.nextbill.core.ui.shortDateText
import java.time.LocalDate

/** 화면 콜백. ViewModel 메서드와 화면 이동(Route가 연결)을 한데 묶습니다. */
internal class DetailActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    /** [DetailUiState.Closed]에서 부릅니다. 여러 번 불러도 한 번만 적용되는 동작이어야 합니다. */
    val onClose: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onOpenNotificationSettings: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

private enum class Confirm { Cancel, Delete }

@Composable
internal fun DetailScreen(
    state: DetailUiState,
    delivery: NotificationDelivery,
    actions: DetailActions,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val currentActions by rememberUpdatedState(actions)
    val closed = state is DetailUiState.Closed
    // 키가 closed라 재구성이 여러 번 일어나도 한 번만 실행됩니다(pop 자체도 멱등).
    LaunchedEffect(closed) {
        if (closed) currentActions.onClose()
    }

    val content = state as? DetailUiState.Content
    val snackbar = remember { SnackbarHostState() }
    val message = content?.message
    val messageText = message?.let { stringResource(it.textRes()) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            currentActions.onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.surfaceCanvas)) {
        Column(Modifier.fillMaxSize()) {
            NbTopAppBar(
                title = stringResource(R.string.detail_title),
                navigationIcon = NbIcons.ArrowBack,
                navigationContentDescription = stringResource(R.string.detail_back),
                onNavigationClick = actions.onBack,
                containerColor = colors.surfaceCard,
                actions = {
                    // 해지한 구독은 읽기 전용이라 수정할 수 없습니다.
                    if (content != null && content.mode !is DetailMode.Cancelled) {
                        IconButton(onClick = actions.onEdit, enabled = !content.busy) {
                            Icon(
                                painterResource(NbIcons.Edit),
                                contentDescription = stringResource(R.string.detail_edit)
                            )
                        }
                    }
                }
            )
            HorizontalDivider(color = colors.borderSubtle)
            if (content != null) DetailBody(content, delivery, actions)
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DetailBody(
    content: DetailUiState.Content,
    delivery: NotificationDelivery,
    actions: DetailActions
) {
    val subscription = content.subscription
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroCard(subscription, content.mode)
        SummaryCard(content, delivery, actions.onOpenNotificationSettings)
        if (content.mode !is DetailMode.Cancelled) UpcomingCard(content.upcoming)
        if (subscription.memo.isNotBlank()) MemoCard(subscription.memo)
        BottomActions(
            mode = content.mode,
            busy = content.busy,
            onCancel = { confirm = Confirm.Cancel },
            onRestore = actions.onRestore,
            onDelete = { confirm = Confirm.Delete }
        )
    }
    when (confirm) {
        Confirm.Cancel -> ConfirmDialog(
            title = R.string.detail_confirm_cancel_title,
            body = R.string.detail_confirm_cancel_body,
            okLabel = R.string.detail_confirm_cancel_ok,
            destructive = false,
            onConfirm = {
                confirm = null
                actions.onCancel()
            },
            onDismiss = { confirm = null }
        )

        Confirm.Delete -> ConfirmDialog(
            title = R.string.detail_confirm_delete_title,
            body = R.string.detail_confirm_delete_body,
            okLabel = R.string.detail_confirm_delete_ok,
            destructive = true,
            onConfirm = {
                confirm = null
                actions.onDelete()
            },
            onDismiss = { confirm = null }
        )

        null -> Unit
    }
}

@Composable
private fun HeroCard(subscription: Subscription, mode: DetailMode) {
    val colors = NextBillTheme.colors
    NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LetterAvatar(subscription.name, subscription.colorKey, size = 64.dp)
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Text(
                    subscription.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                )
                Text(
                    categoryLabel(subscription.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .background(colors.badgeNeutralBg, CircleShape)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (mode is DetailMode.Cancelled) {
                Text(
                    stringResource(R.string.detail_cancelled_on, monthDayText(mode.cancelledAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider(
                color = colors.borderSubtle,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
            MoneyText(
                money = subscription.price,
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painterResource(NbIcons.CalendarMonth),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    stringResource(
                        R.string.detail_cycle_billing,
                        cycleText(subscription.cycle, subscription.anchorDate)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = NextBillTheme.colors.textTertiary,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
private fun SummaryCard(
    content: DetailUiState.Content,
    delivery: NotificationDelivery,
    onOpenSettings: () -> Unit
) {
    NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        SectionTitle(stringResource(R.string.detail_summary_title))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EquivalentTile(
                R.string.detail_monthly,
                NbIcons.CalendarMonth,
                content.monthly,
                Modifier.weight(1f)
            )
            EquivalentTile(
                R.string.detail_yearly,
                R.drawable.detail_ic_calendar_today,
                content.yearly,
                Modifier.weight(1f)
            )
        }
        ReminderRow(
            reminder = content.subscription.reminder,
            // 해지한 구독의 알림은 오지 않으므로 수신 가능 여부를 따지지 않고 설정값만 보여 줍니다.
            delivery = if (content.mode is DetailMode.Cancelled) {
                NotificationDelivery.Available
            } else {
                delivery
            },
            showBadge = content.mode !is DetailMode.Cancelled,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun EquivalentTile(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    value: Money,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceCanvas.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
        }
        MoneyText(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    delivery: NotificationDelivery,
    showBadge: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val unavailable = reminder.enabled && delivery != NotificationDelivery.Available
    // 시스템 대화상자를 더 띄울 수 없을 때만 설정으로 안내합니다(ADR-5). 아직 물어볼 수 있으면 편집 화면의 몫입니다.
    val canOpenSettings = delivery == NotificationDelivery.BlockedInSettings ||
        delivery == NotificationDelivery.NeedsPermission(canAsk = false)
    val valueText = when {
        !reminder.enabled -> stringResource(R.string.detail_reminder_off)

        unavailable -> stringResource(R.string.detail_reminder_unavailable)

        else -> stringResource(
            R.string.detail_reminder_value,
            daysBeforeLabel(reminder.daysBefore),
            reminderTimeText(reminder.time)
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceCanvas.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceCard)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    painterResource(NbIcons.Notifications),
                    contentDescription = null,
                    tint = if (unavailable) {
                        colors.urgentText
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(R.string.detail_reminder_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
                Text(
                    valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (unavailable) colors.urgentText else colors.textPrimary
                )
            }
            if (showBadge && reminder.enabled && !unavailable) {
                NbPill(stringResource(R.string.detail_reminder_on_badge))
            }
        }
        if (unavailable && canOpenSettings) {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.heightIn(min = 48.dp).align(Alignment.End)
            ) {
                Text(
                    stringResource(R.string.detail_reminder_open_settings),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun UpcomingCard(upcoming: List<Pair<LocalDate, DueLabel>>) {
    val colors = NextBillTheme.colors
    NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painterResource(NbIcons.CalendarMonth),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                stringResource(R.string.detail_upcoming_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text(
                stringResource(R.string.detail_upcoming_caption),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textTertiary
            )
        }
        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            upcoming.forEachIndexed { index, (date, label) ->
                UpcomingRow(date, label, isNext = index == 0)
            }
        }
    }
}

@Composable
private fun UpcomingRow(date: LocalDate, label: DueLabel, isNext: Boolean) {
    val colors = NextBillTheme.colors
    val urgent = label == DueLabel.Today || label == DueLabel.Tomorrow
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (urgent) colors.urgentSurface else colors.surfaceCard)
            .border(
                1.dp,
                if (urgent) colors.urgentText.copy(alpha = 0.2f) else colors.borderSubtle,
                shape
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (urgent) colors.urgentText else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                shortDateText(date),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (urgent) FontWeight.Bold else FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                stringResource(
                    if (isNext) R.string.detail_upcoming_next else R.string.detail_upcoming_regular
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = if (urgent) colors.textSecondary else colors.textTertiary
            )
        }
        DueChip(label)
    }
}

@Composable
private fun MemoCard(memo: String) {
    val colors = NextBillTheme.colors
    NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Text(
            stringResource(R.string.detail_memo_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            memo,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceCanvas.copy(alpha = 0.7f))
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp)
        )
    }
}

@Composable
private fun BottomActions(
    mode: DetailMode,
    busy: Boolean,
    onCancel: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (mode) {
            DetailMode.Active -> NbSecondaryButton(
                text = stringResource(R.string.detail_action_cancel),
                onClick = onCancel,
                enabled = !busy,
                leadingIcon = R.drawable.detail_ic_cancel,
                modifier = Modifier.fillMaxWidth()
            )

            DetailMode.BeforeFirstBilling -> Text(
                stringResource(R.string.detail_before_first_billing),
                style = MaterialTheme.typography.bodySmall,
                color = NextBillTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            is DetailMode.Cancelled -> NbPrimaryButton(
                text = stringResource(R.string.detail_action_restore),
                onClick = onRestore,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        NbDestructiveTextButton(
            text = stringResource(R.string.detail_action_delete),
            onClick = onDelete,
            enabled = !busy,
            leadingIcon = NbIcons.Delete,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: Int,
    body: Int,
    okLabel: Int,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(okLabel),
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_confirm_dismiss))
            }
        }
    )
}

private fun DetailMessage.textRes(): Int = when (this) {
    DetailMessage.PERIOD_LIMIT -> R.string.detail_message_period_limit
    DetailMessage.NOT_READY -> R.string.detail_message_not_ready
    DetailMessage.STORAGE_FAILED -> R.string.detail_message_storage_failed
}
