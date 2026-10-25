package dev.lioracode.nextbill.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * 탭마다 화면 기록(sub stack)을 따로 둡니다. 모든 스택은 [rememberNavBackStack]으로 저장되므로
 * 구성 변경과 프로세스 재생성 뒤에도 복원됩니다. [topLevelKeys]의 순서는 바뀌지 않아야 합니다.
 */
@Composable
fun rememberNavigationState(startKey: NavKey, topLevelKeys: List<NavKey>): NavigationState {
    require(startKey in topLevelKeys) { "startKey must be one of topLevelKeys" }
    val topLevelStack = rememberNavBackStack(startKey)
    // 탭마다 key()로 묶어, 저장 상태가 위치(composite key hash)가 아니라 탭 키에 묶이게 합니다.
    val subStacks = topLevelKeys.associateWith { tabKey ->
        key(tabKey) { rememberNavBackStack(tabKey) }
    }
    return remember(startKey, topLevelStack, subStacks) {
        NavigationState(startKey, topLevelStack, subStacks)
    }
}

/**
 * [topLevelStack]은 지나온 탭입니다: 시작 탭이면 `[start]`, 다른 탭이면 `[start, tab]`.
 * [subStacks]의 각 스택은 그 탭의 루트 키로 시작하고 비지 않습니다.
 */
class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    val topLevelKeys: Set<NavKey> get() = subStacks.keys

    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks.getValue(currentTopLevelKey)

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }

    /** 탭 루트에 있을 때만 하단 바를 보여 줍니다. */
    val isAtTopLevel: Boolean by derivedStateOf { currentKey == currentTopLevelKey }

    /**
     * 화면에 넘길 엔트리: 지나온 탭의 스택을 순서대로 이은 것입니다. 엔트리마다 저장 상태(스크롤·선택)와
     * ViewModelStore를 따로 두고, 스택에서 빠지면 둘 다 해제합니다.
     */
    @Composable
    fun toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        // 같은 키(예: DetailNavKey(1))가 두 탭에 있어도 탭마다 저장 상태·ViewModelStore를 따로 둡니다.
        val decorated = subStacks.mapValues { (tabKey, stack) ->
            key(tabKey) {
                val decorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>()
                )
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = decorators,
                    entryProvider = entryProvider
                )
            }
        }
        return topLevelStack.flatMap { decorated[it].orEmpty() }
    }
}
