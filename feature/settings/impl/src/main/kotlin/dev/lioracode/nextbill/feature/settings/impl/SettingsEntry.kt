package dev.lioracode.nextbill.feature.settings.impl

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.ui.NotificationDelivery
import dev.lioracode.nextbill.core.ui.openAppNotificationSettings
import dev.lioracode.nextbill.core.ui.rememberNotificationDelivery
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.settings.api.LicensesNavKey
import dev.lioracode.nextbill.feature.settings.api.SettingsNavKey
import dev.lioracode.nextbill.feature.settings.impl.backup.readBackup
import dev.lioracode.nextbill.feature.settings.impl.backup.rememberBackupLaunchers
import dev.lioracode.nextbill.feature.settings.impl.backup.writeBackup
import dev.lioracode.nextbill.feature.settings.impl.licenses.LicensesRoute

/** 설정 탭 루트와 오픈소스 라이선스. 해지한 구독·라이선스는 설정 탭 스택에 쌓입니다. */
fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsRoute(
            onOpenArchive = { navigator.navigate(ArchiveNavKey) },
            onOpenLicenses = { navigator.navigate(LicensesNavKey) }
        )
    }
    entry<LicensesNavKey> { LicensesRoute(onBack = { navigator.pop(LicensesNavKey) }) }
}

@Composable
private fun SettingsRoute(
    onOpenArchive: () -> Unit,
    onOpenLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestedBefore by viewModel.permissionRequestedBefore.collectAsStateWithLifecycle()
    // ON_RESUME마다 다시 계산하므로 설정 앱에서 바꾸고 돌아오면 반영됩니다.
    val delivery by rememberNotificationDelivery(requestedBefore)
    val context = LocalContext.current
    val resolver = context.contentResolver
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onPermissionRequestResult()
        }
    // 고른 가져오기 파일. 확인 대화상자가 떠 있는 동안 회전해도 남습니다.
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launchers = rememberBackupLaunchers(
        onExportTo = { uri -> viewModel.export { text -> resolver.writeBackup(uri, text) } },
        onImportFrom = { uri -> pendingImport = uri }
    )
    // delivery는 State 위임이라 람다가 불릴 때의 최신 값을 읽습니다.
    val actions =
        remember(viewModel, launchers, permissionLauncher, onOpenArchive, onOpenLicenses) {
            SettingsActions(
                onDefaultReminder = viewModel::setDefaultReminder,
                onTheme = viewModel::setTheme,
                onDynamicColor = viewModel::setDynamicColor,
                onCurrency = viewModel::setDefaultCurrency,
                onNotificationStatus = {
                    val canAsk = delivery == NotificationDelivery.NeedsPermission(canAsk = true)
                    if (canAsk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.openAppNotificationSettings()
                    }
                },
                onExport = { launchers.export(viewModel.backupFileName()) },
                onImport = launchers.import,
                onConfirmImport = {
                    val uri = pendingImport
                    pendingImport = null
                    if (uri != null) viewModel.import { resolver.readBackup(uri) }
                },
                onDismissImport = { pendingImport = null },
                onOpenArchive = onOpenArchive,
                onOpenLicenses = onOpenLicenses,
                onMessageShown = viewModel::messageShown
            )
        }
    SettingsScreen(
        state = state,
        delivery = delivery,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        importPending = pendingImport != null,
        actions = actions
    )
}
