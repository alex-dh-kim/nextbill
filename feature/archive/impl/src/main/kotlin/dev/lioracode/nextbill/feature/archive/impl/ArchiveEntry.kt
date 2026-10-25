package dev.lioracode.nextbill.feature.archive.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey

fun EntryProviderScope<NavKey>.archiveEntry(navigator: Navigator) {
    entry<ArchiveNavKey> {
        ArchiveRoute(
            onBack = { navigator.pop(ArchiveNavKey) },
            onOpenDetail = { id -> navigator.navigate(DetailNavKey(id.value)) }
        )
    }
}

@Composable
private fun ArchiveRoute(
    onBack: () -> Unit,
    onOpenDetail: (SubscriptionId) -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack, onOpenDetail) {
        ArchiveActions(
            onBack = onBack,
            onOpenDetail = onOpenDetail,
            onRestore = viewModel::restore,
            onDelete = viewModel::delete,
            onMessageShown = viewModel::messageShown
        )
    }
    ArchiveScreen(state = state, actions = actions)
}
