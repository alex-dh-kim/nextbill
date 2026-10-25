package dev.lioracode.nextbill.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// feature api 모듈은 core:navigation을 의존하므로 여기서는 같은 모양의 테스트 키를 씁니다.
private data object Home : NavKey

private data object Calendar : NavKey

private data object Settings : NavKey

private data object Archive : NavKey

private data class Detail(val id: Long) : NavKey

private data class Editor(val id: Long? = null) : NavKey

class NavigatorTest {
    private val state = NavigationState(
        startKey = Home,
        topLevelStack = NavBackStack(Home),
        subStacks = listOf(Home, Calendar, Settings).associateWith { NavBackStack(it) }
    )
    private val navigator = Navigator(state)

    private fun stackOf(tab: NavKey): List<NavKey> = state.subStacks.getValue(tab).toList()

    @Test
    fun pushOnCurrentTab() {
        navigator.navigate(Detail(1))

        assertThat(state.currentTopLevelKey).isEqualTo(Home)
        assertThat(state.currentKey).isEqualTo(Detail(1))
        assertThat(stackOf(Home)).containsExactly(Home, Detail(1)).inOrder()
        assertThat(state.isAtTopLevel).isFalse()
    }

    @Test
    fun switchTabKeepsOtherStacks() {
        navigator.navigate(Settings)
        navigator.navigate(Archive)
        navigator.navigate(Home)
        assertThat(state.currentKey).isEqualTo(Home)

        navigator.navigate(Settings)

        assertThat(state.currentKey).isEqualTo(Archive)
        assertThat(state.currentTopLevelKey).isEqualTo(Settings)
    }

    @Test
    fun reselectTabPopsToRoot() {
        navigator.navigate(Settings)
        navigator.navigate(Archive)

        navigator.navigate(Settings)

        assertThat(state.currentKey).isEqualTo(Settings)
        assertThat(stackOf(Settings)).containsExactly(Settings)
        assertThat(state.isAtTopLevel).isTrue()
    }

    @Test
    fun backFromNonStartTabRootGoesHome() {
        navigator.navigate(Calendar)

        navigator.goBack()

        assertThat(state.currentTopLevelKey).isEqualTo(Home)
        assertThat(state.currentKey).isEqualTo(Home)
        assertThat(state.topLevelStack.toList()).containsExactly(Home)
    }

    @Test
    fun backAfterSwitchingBetweenNonStartTabsGoesHome() {
        navigator.navigate(Settings)
        navigator.navigate(Calendar)

        navigator.goBack()

        assertThat(state.currentTopLevelKey).isEqualTo(Home)
        assertThat(state.topLevelStack.toList()).containsExactly(Home)
    }

    @Test
    fun backPopsPushedScreenBeforeLeavingTab() {
        navigator.navigate(Settings)
        navigator.navigate(Archive)

        navigator.goBack()

        assertThat(state.currentTopLevelKey).isEqualTo(Settings)
        assertThat(state.currentKey).isEqualTo(Settings)
    }

    @Test
    fun backAtStartRootDoesNothing() {
        navigator.goBack()

        assertThat(state.currentKey).isEqualTo(Home)
        assertThat(state.topLevelStack.toList()).containsExactly(Home)
    }

    @Test
    fun sameKeyIsNotStackedTwice() {
        navigator.navigate(Detail(1))
        navigator.navigate(Editor(1))
        navigator.navigate(Detail(1))

        assertThat(stackOf(Home)).containsExactly(Home, Editor(1), Detail(1)).inOrder()
    }

    @Test
    fun popIsIdempotent() {
        navigator.navigate(Detail(1))

        navigator.pop(Detail(1))
        navigator.pop(Detail(1))

        assertThat(stackOf(Home)).containsExactly(Home)
        assertThat(state.topLevelStack.toList()).containsExactly(Home)
    }

    @Test
    fun popIgnoresKeyThatIsNotOnTop() {
        navigator.navigate(Detail(1))
        navigator.navigate(Editor(1))

        navigator.pop(Detail(1))

        assertThat(stackOf(Home)).containsExactly(Home, Detail(1), Editor(1)).inOrder()
    }

    @Test
    fun popNeverRemovesTabRoot() {
        navigator.navigate(Settings)

        navigator.pop(Settings)

        assertThat(state.currentTopLevelKey).isEqualTo(Settings)
        assertThat(stackOf(Settings)).containsExactly(Settings)
    }

    @Test
    fun resetToRootsClearsAllTabs() {
        navigator.navigate(Detail(1))
        navigator.navigate(Settings)
        navigator.navigate(Archive)

        navigator.resetToRoots()

        assertThat(stackOf(Home)).containsExactly(Home)
        assertThat(stackOf(Settings)).containsExactly(Settings)
        assertThat(stackOf(Calendar)).containsExactly(Calendar)
        assertThat(state.currentTopLevelKey).isEqualTo(Settings)
        assertThat(state.currentKey).isEqualTo(Settings)
    }
}
