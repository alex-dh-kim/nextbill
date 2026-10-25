package dev.lioracode.nextbill.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
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
class SettingsDataSourceTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun TestScope.store() = PreferenceDataStoreFactory.create(
        scope = backgroundScope
    ) { tmpFolder.newFile("t.preferences_pb") }

    private suspend fun DataStore<Preferences>.put(block: (MutablePreferences) -> Unit) {
        edit { block(it) }
    }

    @Test
    fun emptyStateIsDefault() = runTest(UnconfinedTestDispatcher()) {
        val source = SettingsDataSource(store())
        assertThat(source.settings.first()).isEqualTo(UserSettings.DEFAULT)
        assertThat(source.notificationPermissionRequested.first()).isFalse()
    }

    @Test
    fun updateIsReflected() = runTest(UnconfinedTestDispatcher()) {
        val source = SettingsDataSource(store())
        val changed = UserSettings.DEFAULT.copy(
            defaultReminder = Reminder(false, 7, LocalTime.of(21, 30)),
            themeMode = ThemeMode.DARK,
            useDynamicColor = true,
            defaultCurrency = CurrencyCode.USD
        )
        source.update { changed }
        assertThat(source.settings.first()).isEqualTo(changed)
    }

    @Test
    fun unknownThemeFallsBackToSystem() = runTest(UnconfinedTestDispatcher()) {
        val store = store()
        store.put { it[stringPreferencesKey("theme_mode")] = "SEPIA" }
        assertThat(SettingsDataSource(store).settings.first().themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun unsupportedCurrencyFallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val store = store()
        store.put { it[stringPreferencesKey("default_currency")] = "GBP" }
        assertThat(SettingsDataSource(store).settings.first().defaultCurrency)
            .isEqualTo(UserSettings.DEFAULT.defaultCurrency)
    }

    @Test
    fun invalidDaysBeforeFallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val store = store()
        store.put { it[intPreferencesKey("default_reminder_days_before")] = 5 }
        assertThat(SettingsDataSource(store).settings.first().defaultReminder.daysBefore)
            .isEqualTo(Reminder.DEFAULT.daysBefore)
    }

    @Test
    fun outOfRangeMinuteOfDayFallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val store = store()
        for (bad in listOf(-1, 1440, 99_999)) {
            store.put { it[intPreferencesKey("default_reminder_minute_of_day")] = bad }
            assertThat(SettingsDataSource(store).settings.first().defaultReminder.time)
                .isEqualTo(Reminder.DEFAULT.time)
        }
    }

    @Test
    fun corruptFileFallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val file = tmpFolder.newFile("bad.preferences_pb")
        file.writeBytes(ByteArray(64) { 0x7F })
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val source = SettingsDataSource(store)
        assertThat(source.settings.first()).isEqualTo(UserSettings.DEFAULT)
        assertThat(source.notificationPermissionRequested.first()).isFalse()
    }

    @Test
    fun sequentialPartialUpdatesBothPersist() = runTest(UnconfinedTestDispatcher()) {
        val source = SettingsDataSource(store())
        source.update { it.copy(themeMode = ThemeMode.LIGHT) }
        source.update { it.copy(defaultCurrency = CurrencyCode.EUR) }
        val result = source.settings.first()
        assertThat(result.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(result.defaultCurrency).isEqualTo(CurrencyCode.EUR)
    }

    @Test
    fun updateLeavesNotificationPermissionFlagUntouched() = runTest(UnconfinedTestDispatcher()) {
        val source = SettingsDataSource(store())
        source.update { it.copy(useDynamicColor = true) }
        assertThat(source.notificationPermissionRequested.first()).isFalse()
        source.markNotificationPermissionRequested()
        source.update { it.copy(themeMode = ThemeMode.DARK) }
        assertThat(source.notificationPermissionRequested.first()).isTrue()
    }

    @Test
    fun markNotificationPermissionRequested() = runTest(UnconfinedTestDispatcher()) {
        val source = SettingsDataSource(store())
        source.markNotificationPermissionRequested()
        assertThat(source.notificationPermissionRequested.first()).isTrue()
    }
}
