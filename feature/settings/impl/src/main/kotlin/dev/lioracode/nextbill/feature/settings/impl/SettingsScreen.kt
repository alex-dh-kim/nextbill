package dev.lioracode.nextbill.feature.settings.impl

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbSwitch
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.core.ui.daysBeforeLabel
import dev.lioracode.nextbill.core.ui.reminderTimeText
import java.time.LocalTime
import java.util.Currency
import java.util.Locale

/** 화면 콜백. ViewModel 메서드, 파일 선택·권한 요청, 화면 이동(Route가 연결)을 한데 묶습니다. */
internal class SettingsActions(
    val onDefaultReminder: (Reminder) -> Unit = {},
    val onTheme: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onCurrency: (CurrencyCode) -> Unit = {},
    val onNotificationStatus: () -> Unit = {},
    /** 저장 위치 선택(CreateDocument)을 엽니다. */
    val onExport: () -> Unit = {},
    /** 가져올 파일 선택(OpenDocument)을 엽니다. 확인 대화상자는 파일을 고른 뒤에 뜹니다. */
    val onImport: () -> Unit = {},
    val onConfirmImport: () -> Unit = {},
    val onDismissImport: () -> Unit = {},
    val onOpenArchive: () -> Unit = {},
    val onOpenLicenses: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

private enum class SettingsDialog { DAYS_BEFORE, TIME, THEME, CURRENCY }

/**
 * 시안 05. 탭 화면이라 뒤로 가기가 없습니다. [importPending]이면 고른 백업 파일을 가져올지 묻습니다.
 * [dynamicColorSupported]가 false(SDK 31 미만)면 동적 색상 행을 숨깁니다.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState?,
    delivery: NotificationDelivery,
    dynamicColorSupported: Boolean,
    importPending: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val currentActions by rememberUpdatedState(actions)

    val snackbar = remember { SnackbarHostState() }
    val message = state?.message
    val messageText = message?.let { messageText(it) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            currentActions.onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.surfaceCanvas)) {
        Column(Modifier.fillMaxSize()) {
            NbTopAppBar(
                title = stringResource(R.string.settings_title),
                titleStyle = MaterialTheme.typography.headlineMedium
            )
            if (state != null) {
                SettingsContent(state, delivery, dynamicColorSupported, actions)
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (importPending) {
        AlertDialog(
            onDismissRequest = actions.onDismissImport,
            text = { Text(stringResource(R.string.settings_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmImport) {
                    Text(stringResource(R.string.settings_import_confirm_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissImport) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    delivery: NotificationDelivery,
    dynamicColorSupported: Boolean,
    actions: SettingsActions
) {
    val colors = NextBillTheme.colors
    val settings = state.settings
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val idle = state.backup == BackupStatus.IDLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NextBillTheme.spacing.margin)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Section(stringResource(R.string.settings_section_notifications)) {
            SettingsRow(
                icon = R.drawable.settings_ic_event_upcoming,
                title = stringResource(R.string.settings_default_days_before),
                value = daysBeforeLabel(settings.defaultReminder.daysBefore),
                onClick = { dialog = SettingsDialog.DAYS_BEFORE }
            )
            RowDivider()
            SettingsRow(
                icon = R.drawable.settings_ic_schedule,
                title = stringResource(R.string.settings_default_time),
                value = reminderTimeText(settings.defaultReminder.time),
                onClick = { dialog = SettingsDialog.TIME }
            )
            RowDivider()
            SettingsRow(
                icon = NbIcons.Notifications,
                title = stringResource(R.string.settings_notification_status),
                onClick = actions.onNotificationStatus,
                chevron = false,
                trailing = { DeliveryPill(delivery == NotificationDelivery.Available) }
            )
        }

        Section(stringResource(R.string.settings_section_display)) {
            SettingsRow(
                icon = R.drawable.settings_ic_dark_mode,
                title = stringResource(R.string.settings_theme),
                value = themeLabel(settings.themeMode),
                onClick = { dialog = SettingsDialog.THEME }
            )
            if (dynamicColorSupported) {
                RowDivider()
                SettingsRow(
                    icon = R.drawable.settings_ic_palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    supporting = stringResource(R.string.settings_dynamic_color_hint),
                    chevron = false,
                    toggle = settings.useDynamicColor to actions.onDynamicColor,
                    trailing = {
                        NbSwitch(checked = settings.useDynamicColor, onCheckedChange = null)
                    }
                )
            }
        }

        Section(stringResource(R.string.settings_section_defaults)) {
            SettingsRow(
                icon = R.drawable.settings_ic_payments,
                title = stringResource(R.string.settings_default_currency),
                value = currencyLabel(settings.defaultCurrency),
                valueColor = colors.textPrimary,
                onClick = { dialog = SettingsDialog.CURRENCY }
            )
        }

        Section(stringResource(R.string.settings_section_data)) {
            SettingsRow(
                icon = R.drawable.settings_ic_upload_file,
                title = stringResource(R.string.settings_export),
                supporting = stringResource(R.string.settings_export_hint),
                onClick = actions.onExport,
                enabled = idle,
                busy = state.backup == BackupStatus.EXPORTING
            )
            RowDivider()
            SettingsRow(
                icon = R.drawable.settings_ic_download,
                title = stringResource(R.string.settings_import),
                supporting = stringResource(R.string.settings_import_hint),
                onClick = actions.onImport,
                enabled = idle,
                busy = state.backup == BackupStatus.IMPORTING
            )
            RowDivider()
            SettingsRow(
                icon = R.drawable.settings_ic_history_toggle_off,
                title = stringResource(R.string.settings_archive),
                onClick = actions.onOpenArchive
            )
        }

        Section(stringResource(R.string.settings_section_about)) {
            SettingsRow(
                icon = R.drawable.settings_ic_policy,
                title = stringResource(R.string.settings_licenses),
                onClick = actions.onOpenLicenses
            )
            RowDivider()
            SettingsRow(
                icon = R.drawable.settings_ic_info,
                title = stringResource(R.string.settings_app_version),
                value = state.appVersion,
                valueColor = colors.textTertiary,
                chevron = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(NbIcons.Lock),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                stringResource(R.string.settings_local_only),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }

    when (dialog) {
        SettingsDialog.DAYS_BEFORE -> ChoiceDialog(
            title = stringResource(R.string.settings_default_days_before),
            options = Reminder.DAYS_BEFORE_OPTIONS,
            selected = settings.defaultReminder.daysBefore,
            label = { daysBeforeLabel(it) },
            onPick = { actions.onDefaultReminder(settings.defaultReminder.copy(daysBefore = it)) },
            onDismiss = { dialog = null }
        )

        SettingsDialog.TIME -> TimeDialog(
            initial = settings.defaultReminder.time,
            onPick = {
                dialog = null
                actions.onDefaultReminder(settings.defaultReminder.copy(time = it))
            },
            onDismiss = { dialog = null }
        )

        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { themeLabel(it) },
            onPick = actions.onTheme,
            onDismiss = { dialog = null }
        )

        SettingsDialog.CURRENCY -> ChoiceDialog(
            title = stringResource(R.string.settings_default_currency),
            options = CurrencyCode.SUPPORTED,
            selected = settings.defaultCurrency,
            label = { currencyLabel(it) },
            onPick = actions.onCurrency,
            onDismiss = { dialog = null }
        )

        null -> Unit
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = NextBillTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() }
        )
        NbCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = NextBillTheme.colors.borderSubtle.copy(alpha = 0.6f))
}

@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    title: String,
    supporting: String? = null,
    value: String? = null,
    valueColor: Color = NextBillTheme.colors.textSecondary,
    onClick: (() -> Unit)? = null,
    toggle: Pair<Boolean, (Boolean) -> Unit>? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    chevron: Boolean = onClick != null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = NextBillTheme.colors
    val interaction = when {
        toggle != null -> Modifier.selectable(
            selected = toggle.first,
            enabled = enabled,
            role = Role.Switch,
            onClick = { toggle.second(!toggle.first) }
        )

        onClick != null -> Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )

        else -> Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    supporting,
                    // 한글이 낱말 중간에서 끊기지 않게 구절 단위로 줄을 바꿉니다(API 33+에서 적용).
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                        lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)
                    ),
                    color = colors.textSecondary
                )
            }
        }
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 1
            )
        }
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        trailing?.invoke()
        if (chevron) {
            Icon(
                painterResource(NbIcons.ChevronRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp).size(20.dp)
            )
        }
    }
}

@Composable
private fun DeliveryPill(available: Boolean) {
    val colors = NextBillTheme.colors
    val (bg, fg, dot) = if (available) {
        Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary
        )
    } else {
        Triple(colors.urgentSurface, colors.urgentText, colors.urgentText)
    }
    Row(
        modifier = Modifier
            .background(bg, NbShapes.Chip)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(6.dp).background(dot, CircleShape))
        Text(
            stringResource(
                if (available) {
                    R.string.settings_notification_allowed
                } else {
                    R.string.settings_notification_unavailable
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onDismiss()
                                    onPick(option)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
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
                Text(stringResource(R.string.settings_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
)

/** "KRW (₩)". 기호는 금액 표시와 같은 `Locale.US` 기준입니다. */
internal fun currencyLabel(code: CurrencyCode): String =
    "${code.value} (${Currency.getInstance(code.value).getSymbol(Locale.US)})"

@Composable
private fun messageText(message: SettingsMessage): String = when (message) {
    SettingsMessage.Exported -> stringResource(R.string.settings_message_exported)

    is SettingsMessage.Imported ->
        pluralStringResource(R.plurals.settings_message_imported, message.count, message.count)

    SettingsMessage.ImportMalformed -> stringResource(R.string.settings_message_import_malformed)

    SettingsMessage.ImportTooLarge -> stringResource(R.string.settings_message_import_too_large)

    is SettingsMessage.ImportUnsupported ->
        stringResource(R.string.settings_message_import_unsupported)

    SettingsMessage.ImportStagingFailed ->
        stringResource(R.string.settings_message_import_staging_failed)

    SettingsMessage.IoFailed -> stringResource(R.string.settings_message_io_failed)

    SettingsMessage.ImportNotReady,
    SettingsMessage.ExportNotReady,
    SettingsMessage.SettingsNotReady -> stringResource(R.string.settings_message_not_ready)

    SettingsMessage.SettingsStorageFailed ->
        stringResource(R.string.settings_message_storage_failed)
}

private const val DISABLED_ALPHA = 0.38f
