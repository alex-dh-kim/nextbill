package dev.lioracode.nextbill.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.datastore.SettingsDataSource
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repo() = DataStoreSettingsRepository(
        SettingsDataSource(
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tmp.newFile("s.preferences_pb")
            }
        )
    )

    @Test
    fun replaceEqualsUpdateWithConstant() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        val s = UserSettings.DEFAULT.copy(
            defaultReminder = Reminder(false, 7, LocalTime.of(21, 30)),
            themeMode = ThemeMode.DARK,
            defaultCurrency = CurrencyCode.EUR
        )
        repo.replace(s)
        assertThat(repo.settings.first()).isEqualTo(s)
        repo.update { it.copy(useDynamicColor = !it.useDynamicColor) }
        assertThat(repo.settings.first()).isEqualTo(s.copy(useDynamicColor = !s.useDynamicColor))
    }

    @Test
    fun permissionFlagPassesThrough() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        assertThat(repo.notificationPermissionRequested.first()).isFalse()
        repo.markNotificationPermissionRequested()
        assertThat(repo.notificationPermissionRequested.first()).isTrue()
    }
}
