package dev.lioracode.nextbill.core.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Serializable
internal data object TabA : NavKey

@Serializable
internal data object TabB : NavKey

@Serializable
internal data class Item(val id: Long) : NavKey

private class CountingViewModel(val serial: Int) : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

/** 같은 키가 두 탭에 쌓여도 엔트리 상태(ViewModel)는 탭마다 따로입니다. */
@RunWith(RobolectricTestRunner::class)
class PerTabEntryStateTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sameKeyInTwoTabsGetsSeparateViewModels() {
        lateinit var navigator: Navigator
        val created = mutableListOf<CountingViewModel>()
        val shownByTab = mutableMapOf<NavKey, CountingViewModel>()

        compose.setContent {
            val state = rememberNavigationState(TabA, listOf(TabA, TabB))
            navigator = remember(state) { Navigator(state) }
            NavDisplay(
                entries = state.toEntries(
                    entryProvider {
                        entry<TabA> { BasicText("A") }
                        entry<TabB> { BasicText("B") }
                        entry<Item> {
                            val vm =
                                viewModel { CountingViewModel(created.size).also(created::add) }
                            shownByTab[state.currentTopLevelKey] = vm
                            BasicText("item ${vm.serial}")
                        }
                    }
                ),
                onBack = navigator::goBack
            )
        }

        compose.runOnIdle { navigator.navigate(Item(1)) }
        compose.onNodeWithText("item 0").assertExists()
        compose.runOnIdle {
            navigator.navigate(TabB)
            navigator.navigate(Item(1))
        }
        compose.onNodeWithText("item 1").assertExists()

        val inA = shownByTab.getValue(TabA)
        val inB = shownByTab.getValue(TabB)
        assertThat(inB).isNotSameInstanceAs(inA)

        compose.runOnIdle { navigator.pop(Item(1)) }
        compose.onNodeWithText("B").assertExists()
        compose.runOnIdle {
            assertThat(inB.cleared).isTrue()
            assertThat(inA.cleared).isFalse()
            navigator.navigate(TabA)
        }

        compose.onNodeWithText("item 0").assertExists()
        assertThat(shownByTab.getValue(TabA)).isSameInstanceAs(inA)
    }
}
