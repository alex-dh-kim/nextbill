package dev.lioracode.nextbill.feature.editor.impl

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.ui.openAppNotificationSettings
import dev.lioracode.nextbill.core.ui.rememberNotificationDelivery
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey

fun EntryProviderScope<NavKey>.editorEntry(navigator: Navigator) {
    entry<EditorNavKey> { key -> EditorRoute(key, onClose = { navigator.pop(key) }) }
}

@Composable
private fun EditorRoute(
    key: EditorNavKey,
    onClose: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel<EditorViewModel, EditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(key) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestedBefore by viewModel.permissionRequestedBefore.collectAsStateWithLifecycle()
    // ON_RESUME마다 다시 계산하므로 설정 앱에서 허용하고 돌아오면 안내가 사라집니다.
    val delivery by rememberNotificationDelivery(requestedBefore)
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionRequestResult()
    }
    // delivery는 State 위임이라 람다가 불릴 때의 최신 값을 읽습니다.
    val actions = remember(viewModel, launcher, onClose) {
        EditorActions(
            onDraftChange = viewModel::update,
            onReminderSwitch = { enabled -> viewModel.onReminderSwitch(enabled, delivery) },
            onSave = { viewModel.onSaveClick(delivery) },
            onBack = onClose,
            onClose = onClose,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.onPermissionRequestResult()
                }
            },
            onPermissionLater = viewModel::onPermissionLater,
            onOpenNotificationSettings = { context.openAppNotificationSettings() },
            onMessageShown = viewModel::messageShown
        )
    }
    EditorScreen(
        state = state,
        delivery = delivery,
        permissionRequestedBefore = requestedBefore,
        actions = actions
    )
}
