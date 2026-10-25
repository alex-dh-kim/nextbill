package dev.lioracode.nextbill.feature.editor.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.model.MAX_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.MIN_ANCHOR_DATE
import dev.lioracode.nextbill.core.ui.LetterAvatar
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.feature.editor.impl.component.AmountField
import dev.lioracode.nextbill.feature.editor.impl.component.CategoryChips
import dev.lioracode.nextbill.feature.editor.impl.component.ColorSwatches
import dev.lioracode.nextbill.feature.editor.impl.component.CycleField
import dev.lioracode.nextbill.feature.editor.impl.component.EditorCard
import dev.lioracode.nextbill.feature.editor.impl.component.FieldErrorText
import dev.lioracode.nextbill.feature.editor.impl.component.FieldLabel
import dev.lioracode.nextbill.feature.editor.impl.component.ReminderSection
import dev.lioracode.nextbill.feature.editor.impl.component.messageRes
import dev.lioracode.nextbill.feature.editor.impl.permission.NotificationPermissionSheet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** 화면 콜백. ViewModel 메서드와 화면 이동(Route가 연결)을 한데 묶습니다. */
internal class EditorActions(
    val onDraftChange: ((SubscriptionDraft) -> SubscriptionDraft) -> Unit = {},
    val onReminderSwitch: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
    /** [EditorPhase.Saved]·[EditorPhase.Missing]에서 부릅니다. 여러 번 불러도 한 번만 적용되는 동작이어야 합니다. */
    val onClose: () -> Unit = {},
    /** OS 권한 대화상자를 띄웁니다. 결과는 ViewModel의 `onPermissionRequestResult()`로 갑니다. */
    val onRequestPermission: () -> Unit = {},
    val onPermissionLater: () -> Unit = {},
    val onOpenNotificationSettings: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
    state: EditorUiState?,
    delivery: NotificationDelivery,
    permissionRequestedBefore: Boolean,
    actions: EditorActions,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val currentActions by rememberUpdatedState(actions)
    val phase = state?.phase
    // 키가 phase라 재구성이 여러 번 일어나도 같은 상태에서는 한 번만 실행됩니다(pop 자체도 멱등).
    LaunchedEffect(phase) {
        if (phase is EditorPhase.Saved || phase == EditorPhase.Missing) currentActions.onClose()
    }

    val snackbar = remember { SnackbarHostState() }
    val message = state?.message
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
                title = stringResource(
                    if (state?.isNew !=
                        false
                    ) {
                        R.string.editor_title_new
                    } else {
                        R.string.editor_title_edit
                    }
                ),
                navigationIcon = NbIcons.ArrowBack,
                navigationContentDescription = stringResource(R.string.editor_back),
                onNavigationClick = actions.onBack,
                containerColor = colors.surfaceCard,
                actions = {
                    TextButton(
                        onClick = actions.onSave,
                        enabled = phase == EditorPhase.Editing,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.editor_save),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (phase == EditorPhase.Editing) 1f else 0.4f
                            )
                        )
                    }
                }
            )
            HorizontalDivider(color = colors.borderSubtle)
            if (state != null) {
                EditorForm(state, delivery, permissionRequestedBefore, actions)
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).imePadding()
        )
    }

    if (state != null && phase is EditorPhase.AskingPermission) {
        NotificationPermissionSheet(
            reminder = state.draft.reminder,
            onAllow = actions.onRequestPermission,
            onLater = actions.onPermissionLater
        )
    }
}

@Composable
private fun EditorForm(
    state: EditorUiState,
    delivery: NotificationDelivery,
    permissionRequestedBefore: Boolean,
    actions: EditorActions
) {
    val draft = state.draft
    val update = actions.onDraftChange
    fun errorFor(vararg fields: FieldError): FieldError? =
        if (state.showErrors) fields.firstOrNull { it in state.errors } else null

    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    // 편집 중이 아니면(저장 중·권한 확인 중·닫히는 중) 입력을 막습니다. ViewModel도 같은 조건으로 무시합니다.
    val enabled = state.phase == EditorPhase.Editing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NextBillTheme.spacing.margin, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NameCard(
            name = draft.name,
            draft = draft,
            error = errorFor(FieldError.NAME_BLANK, FieldError.NAME_TOO_LONG),
            onNameChange = { name -> update { it.copy(name = name) } },
            enabled = enabled
        )
        AmountField(
            amountText = draft.amountText,
            currency = draft.currency,
            error = errorFor(
                FieldError.AMOUNT_BLANK,
                FieldError.AMOUNT_INVALID,
                FieldError.AMOUNT_TOO_LARGE
            ),
            onAmountChange = { text -> update { it.copy(amountText = text) } },
            onCurrencyChange = { code -> update { it.copy(currency = code) } },
            enabled = enabled
        )
        CycleField(
            interval = draft.interval,
            unit = draft.unit,
            anchor = draft.anchorDate,
            showError = state.showErrors,
            onIntervalChange = { n -> update { it.copy(interval = n) } },
            onUnitChange = { u -> update { it.copy(unit = u) } },
            enabled = enabled
        )
        AnchorDateCard(
            date = draft.anchorDate,
            error = errorFor(FieldError.ANCHOR_OUT_OF_RANGE),
            onClick = { pickingDate = true },
            enabled = enabled
        )
        CategoryChips(
            selected = draft.category,
            onSelect = { c -> update { it.copy(category = c) } },
            enabled = enabled
        )
        ColorSwatches(
            selected = draft.colorKey,
            onSelect = { k -> update { it.copy(colorKey = k) } },
            enabled = enabled
        )
        ReminderSection(
            reminder = draft.reminder,
            showUnavailable = showUnavailable(delivery, permissionRequestedBefore),
            onEnabledChange = actions.onReminderSwitch,
            onDaysBeforeChange = { days ->
                update { it.copy(reminder = it.reminder.copy(daysBefore = days)) }
            },
            onTimeClick = { pickingTime = true },
            onUnavailableClick = {
                if (delivery == NotificationDelivery.NeedsPermission(canAsk = true)) {
                    actions.onRequestPermission()
                } else {
                    actions.onOpenNotificationSettings()
                }
            },
            enabled = enabled
        )
        MemoCard(
            memo = draft.memo,
            error = errorFor(FieldError.MEMO_TOO_LONG),
            onMemoChange = { memo -> update { it.copy(memo = memo) } },
            enabled = enabled
        )
    }

    if (pickingDate && enabled) {
        AnchorDatePicker(
            initial = draft.anchorDate,
            onPick = { date ->
                pickingDate = false
                update { it.copy(anchorDate = date) }
            },
            onDismiss = { pickingDate = false }
        )
    }
    if (pickingTime && enabled) {
        ReminderTimePicker(
            initial = draft.reminder.time,
            onPick = { time ->
                pickingTime = false
                update { it.copy(reminder = it.reminder.copy(time = time)) }
            },
            onDismiss = { pickingTime = false }
        )
    }
}

/**
 * 알림 의도는 켜져 있지만 받을 수 없을 때의 안내. OS 대화상자를 아직 한 번도 띄우지 않았다면(저장할 때
 * 안내 시트로 물어볼 것이므로) 미리 보여 주지 않습니다.
 */
internal fun showUnavailable(delivery: NotificationDelivery, requestedBefore: Boolean): Boolean =
    when (delivery) {
        NotificationDelivery.Available -> false
        NotificationDelivery.BlockedInSettings -> true
        is NotificationDelivery.NeedsPermission -> !delivery.canAsk || requestedBefore
    }

@Composable
private fun NameCard(
    name: String,
    draft: SubscriptionDraft,
    error: FieldError?,
    onNameChange: (String) -> Unit,
    enabled: Boolean
) {
    val colors = NextBillTheme.colors
    val label = stringResource(R.string.editor_name_label)
    val errorText = error?.let { stringResource(it.messageRes()) }
    EditorCard(isError = error != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LetterAvatar(name = name, colorKey = draft.colorKey, size = 48.dp)
            Column(Modifier.weight(1f)) {
                FieldLabel(label, Modifier.padding(bottom = 4.dp))
                val style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = label
                            if (errorText != null) error(errorText)
                        },
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text(
                                stringResource(R.string.editor_name_placeholder),
                                style = style.copy(color = colors.textTertiary)
                            )
                        }
                        inner()
                    }
                )
            }
        }
        if (error != null) FieldErrorText(error)
    }
}

@Composable
private fun AnchorDateCard(
    date: LocalDate,
    error: FieldError?,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val colors = NextBillTheme.colors
    val locale = LocalLocale.current.platformLocale
    val pattern = stringResource(R.string.editor_anchor_pattern)
    val text =
        remember(date, pattern, locale) {
            DateTimeFormatter.ofPattern(pattern, locale).format(date)
        }
    EditorCard(
        isError = error != null,
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.editor_pick_date),
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                FieldLabel(
                    stringResource(R.string.editor_anchor_label),
                    Modifier.padding(bottom = 4.dp)
                )
                Text(text, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceCanvas, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    painterResource(NbIcons.CalendarMonth),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (error != null) FieldErrorText(error)
    }
}

@Composable
private fun MemoCard(
    memo: String,
    error: FieldError?,
    onMemoChange: (String) -> Unit,
    enabled: Boolean
) {
    val colors = NextBillTheme.colors
    val label = stringResource(R.string.editor_memo_label)
    val errorText = error?.let { stringResource(it.messageRes()) }
    val style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
    EditorCard(isError = error != null, verticalSpacing = 4.dp) {
        FieldLabel(label)
        BasicTextField(
            value = memo,
            onValueChange = onMemoChange,
            enabled = enabled,
            minLines = 3,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (errorText != null) error(errorText)
                },
            decorationBox = { inner ->
                if (memo.isEmpty()) {
                    Text(
                        stringResource(R.string.editor_memo_placeholder),
                        style = style.copy(color = colors.textTertiary)
                    )
                }
                inner()
            }
        )
        if (error != null) FieldErrorText(error)
    }
}

/** M3 DatePicker는 UTC 자정 millis를 씁니다. `ZoneOffset.UTC`로만 변환합니다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnchorDatePicker(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        yearRange = MIN_ANCHOR_DATE.year..MAX_ANCHOR_DATE.year
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(it.toUtcDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.editor_dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_dialog_cancel)) }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.editor_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_dialog_cancel)) }
        },
        text = { TimePicker(state = state) }
    )
}

internal fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun EditorMessage.textRes(): Int = when (this) {
    EditorMessage.LIMIT_REACHED -> R.string.editor_message_limit_reached
    EditorMessage.STORAGE_FAILED -> R.string.editor_message_storage_failed
    EditorMessage.NOT_READY -> R.string.editor_message_not_ready
}
