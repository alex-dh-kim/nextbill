package dev.lioracode.nextbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.lioracode.nextbill.core.designsystem.component.NbNavItem
import dev.lioracode.nextbill.core.designsystem.component.NbNavigationBar
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.navigation.NavigationState
import dev.lioracode.nextbill.core.navigation.Navigator
import dev.lioracode.nextbill.core.navigation.rememberNavigationState
import dev.lioracode.nextbill.feature.archive.api.ArchiveNavKey
import dev.lioracode.nextbill.feature.archive.impl.archiveEntry
import dev.lioracode.nextbill.feature.calendar.api.CalendarNavKey
import dev.lioracode.nextbill.feature.calendar.impl.calendarEntry
import dev.lioracode.nextbill.feature.detail.api.DetailNavKey
import dev.lioracode.nextbill.feature.detail.impl.detailEntry
import dev.lioracode.nextbill.feature.editor.api.EditorNavKey
import dev.lioracode.nextbill.feature.editor.impl.editorEntry
import dev.lioracode.nextbill.feature.home.api.HomeNavKey
import dev.lioracode.nextbill.feature.home.impl.homeEntry
import dev.lioracode.nextbill.feature.settings.api.LicensesNavKey
import dev.lioracode.nextbill.feature.settings.api.SettingsNavKey
import dev.lioracode.nextbill.feature.settings.impl.settingsEntry

internal const val BOTTOM_BAR_TAG = "bottomBar"

@Composable
internal fun rememberNextBillNavigationState(): NavigationState = rememberNavigationState(
    startKey = TopLevelDestination.entries.first().key,
    topLevelKeys = TopLevelDestination.entries.map { it.key }
)

/** 탭 3개짜리 앱 셸. [dataGeneration]이 바뀌면(가져오기) 모든 탭 스택을 루트로 되돌립니다. */
@Composable
fun NextBillApp(dataGeneration: Long, modifier: Modifier = Modifier) {
    val state = rememberNextBillNavigationState()
    val navigator = remember(state) { Navigator(state) }
    NextBillApp(navigator, dataGeneration, modifier)
}

@Composable
internal fun NextBillApp(
    navigator: Navigator,
    dataGeneration: Long,
    modifier: Modifier = Modifier,
    entries: (Navigator) -> (NavKey) -> NavEntry<NavKey> = ::appEntries
) {
    val state = navigator.state
    // 화면 기록과 함께 저장해, 재생성 뒤 같은 세대면 되돌리지 않습니다.
    var shownGeneration by rememberSaveable { mutableLongStateOf(dataGeneration) }
    LaunchedEffect(dataGeneration) {
        if (dataGeneration != shownGeneration) {
            navigator.resetToRoots()
            shownGeneration = dataGeneration
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = NextBillTheme.colors.surfaceCanvas,
        bottomBar = {
            if (state.isAtTopLevel) {
                val destinations = TopLevelDestination.entries
                NbNavigationBar(
                    items = destinations.map { NbNavItem(stringResource(it.label), it.icon) },
                    selected = destinations.indexOfFirst { it.key == state.currentTopLevelKey },
                    onSelect = { navigator.navigate(destinations[it].key) },
                    modifier = Modifier.testTag(BOTTOM_BAR_TAG)
                )
            }
        }
    ) { padding ->
        NavDisplay(
            entries = state.toEntries(entries(navigator)),
            onBack = navigator::goBack,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        )
    }
}

/** 모든 화면의 실제 진입점. */
internal fun appEntries(navigator: Navigator) = entryProvider<NavKey> {
    homeEntry(navigator)
    calendarEntry(navigator)
    editorEntry(navigator)
    detailEntry(navigator)
    archiveEntry(navigator)
    settingsEntry(navigator)
}

/** Hilt 없이 앱 셸을 시험하기 위한 전부 자리표시자인 진입점(홈 포함). */
internal fun placeholderEntries(navigator: Navigator) = entryProvider<NavKey> {
    entry<HomeNavKey> { Placeholder(it, navigator, DetailNavKey(1), EditorNavKey()) }
    entry<CalendarNavKey> { Placeholder(it, navigator, DetailNavKey(1)) }
    entry<EditorNavKey> { Placeholder(it, navigator) }
    entry<DetailNavKey> { Placeholder(it, navigator, EditorNavKey(it.subscriptionId)) }
    entry<ArchiveNavKey> { Placeholder(it, navigator, DetailNavKey(1)) }
    entry<SettingsNavKey> { Placeholder(it, navigator, ArchiveNavKey, LicensesNavKey) }
    entry<LicensesNavKey> { Placeholder(it, navigator) }
}

internal fun openLabel(key: NavKey) = "Open $key"

@Composable
private fun Placeholder(key: NavKey, navigator: Navigator, vararg links: NavKey) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(key.toString(), style = MaterialTheme.typography.titleLarge)
        links.forEach { link ->
            TextButton(onClick = { navigator.navigate(link) }) { Text(openLabel(link)) }
        }
    }
}
