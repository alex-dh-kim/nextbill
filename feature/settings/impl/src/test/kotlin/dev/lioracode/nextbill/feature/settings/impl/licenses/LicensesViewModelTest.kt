package dev.lioracode.nextbill.feature.settings.impl.licenses

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LicensesViewModelTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun loadsBundledCatalogOnIoDispatcher() {
        val vm = LicensesViewModel(ApplicationProvider.getApplicationContext(), main.dispatcher)
        val catalog = vm.catalog.value!!
        assertThat(catalog.entries.map { it.name }).contains("Pretendard")
        assertThat(catalog.texts.keys).containsExactly("Apache-2.0", "OFL-1.1", "BSD-3-Clause")
    }
}
