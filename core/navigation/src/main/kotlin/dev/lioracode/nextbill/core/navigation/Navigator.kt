package dev.lioracode.nextbill.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 화면 이동. 화면(Composable)만 부르고 ViewModel은 부르지 않습니다. ViewModel은 결과를 UiState로
 * 내보내고, 화면이 그 상태를 보고 [pop]처럼 여러 번 불러도 한 번만 적용되는 메서드를 부릅니다.
 */
class Navigator(val state: NavigationState) {
    /**
     * - 지금 탭의 루트 키: 그 탭을 루트로 되돌립니다(같은 탭 재선택).
     * - 다른 탭의 루트 키: 그 탭으로 바꿉니다. 그 탭의 기록은 그대로입니다.
     * - 그 밖의 키: 지금 탭에 쌓습니다. 이미 스택에 있으면 쌓지 않고 맨 위로 옮깁니다.
     */
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack(state.currentSubStack)

            in state.topLevelKeys -> goToTopLevel(key)

            else -> state.currentSubStack.apply {
                remove(key)
                add(key)
            }
        }
    }

    /** 쌓인 화면이 있으면 하나 닫고, 시작 탭이 아닌 탭의 루트면 시작 탭으로 갑니다. 시작 탭 루트면 무시합니다. */
    fun goBack() {
        val subStack = state.currentSubStack
        when {
            subStack.size > 1 -> subStack.removeAt(subStack.lastIndex)

            state.currentTopLevelKey != state.startKey ->
                state.topLevelStack.removeAt(state.topLevelStack.lastIndex)
        }
    }

    /** 지금 맨 위가 [key]일 때만 닫습니다. 탭 루트는 닫지 않습니다. 여러 번 불러도 한 번만 적용됩니다. */
    fun pop(key: NavKey) {
        val subStack = state.currentSubStack
        if (subStack.size > 1 && subStack.last() == key) subStack.removeAt(subStack.lastIndex)
    }

    /** 모든 탭의 기록을 루트로 되돌립니다(가져오기로 id가 새로 매겨진 뒤). 지금 탭은 그대로입니다. */
    fun resetToRoots() {
        state.subStacks.values.forEach(::clearSubStack)
    }

    private fun goToTopLevel(key: NavKey) {
        // 지나온 탭은 [시작 탭] 또는 [시작 탭, 지금 탭]뿐이라, 다른 탭 루트에서 뒤로 가면 항상 시작 탭입니다.
        // 시작 탭은 맨 아래에 그대로 두고 그 위만 바꿔 스택이 비는 순간이 없게 합니다.
        val stack = state.topLevelStack
        clearSubStack(stack)
        if (key != state.startKey) stack.add(key)
    }

    private fun clearSubStack(stack: MutableList<NavKey>) {
        if (stack.size > 1) stack.subList(1, stack.size).clear()
    }
}
