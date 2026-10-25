package dev.lioracode.nextbill.feature.detail.impl

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
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey

fun EntryProviderScope<NavKey>.detailEntry(navigator: Navigator) {
    entry<DetailNavKey> { key ->
        DetailRoute(
            key,
            onBack = { navigator.pop(key) },
            onEdit = { navigator.navigate(EditorNavKey(key.subscriptionId)) }
        )
    }
}

@Composable
private fun DetailRoute(
    key: DetailNavKey,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(key) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestedBefore by viewModel.permissionRequestedBefore.collectAsStateWithLifecycle()
    // ON_RESUME마다 다시 계산하므로 설정 앱에서 허용하고 돌아오면 안내가 사라집니다.
    val delivery by rememberNotificationDelivery(requestedBefore)
    val context = LocalContext.current
    val actions = remember(viewModel, onBack, onEdit, context) {
        DetailActions(
            onBack = onBack,
            onEdit = onEdit,
            onClose = onBack,
            onCancel = viewModel::cancel,
            onRestore = viewModel::restore,
            onDelete = viewModel::delete,
            onOpenNotificationSettings = { context.openAppNotificationSettings() },
            onMessageShown = viewModel::messageShown
        )
    }
    DetailScreen(state = state, delivery = delivery, actions = actions)
}
