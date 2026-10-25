package dev.lioracode.nextbill.feature.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey
import dev.lioracode.nextbill.feature.home.api.HomeNavKey

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> { HomeRoute(navigator) }
}

@Composable
private fun HomeRoute(navigator: Navigator, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onAddClick = { navigator.navigate(EditorNavKey()) },
        onItemClick = { navigator.navigate(DetailNavKey(it.value)) }
    )
}
