package dev.lioracode.nextbill.feature.calendar.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.feature.calendar.api.CalendarNavKey
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey

fun EntryProviderScope<NavKey>.calendarEntry(navigator: Navigator) {
    entry<CalendarNavKey> { CalendarRoute(navigator) }
}

@Composable
private fun CalendarRoute(navigator: Navigator, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::goToToday,
        onSelect = viewModel::select,
        onPaymentClick = { navigator.navigate(DetailNavKey(it.value)) }
    )
}
