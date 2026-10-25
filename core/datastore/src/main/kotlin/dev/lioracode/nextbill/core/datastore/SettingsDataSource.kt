package dev.lioracode.nextbill.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** 앱 설정을 Preferences DataStore에 저장합니다. 값이 없거나 알 수 없으면 [UserSettings.DEFAULT]의 값을 씁니다. */
class SettingsDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {
    /** 읽을 수 없거나 손상된 파일은 빈 값으로 보고 기본값으로 대체합니다. */
    private val data: Flow<Preferences> = dataStore.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }

    val settings: Flow<UserSettings> = data.map { it.toSettings() }

    val notificationPermissionRequested: Flow<Boolean> =
        data.map { it[NOTIFICATION_PERMISSION_REQUESTED] ?: false }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[REMINDER_ENABLED] = next.defaultReminder.enabled
            prefs[REMINDER_DAYS_BEFORE] = next.defaultReminder.daysBefore
            prefs[REMINDER_MINUTE_OF_DAY] = next.defaultReminder.time.toSecondOfDay() / 60
            prefs[THEME_MODE] = next.themeMode.name
            prefs[USE_DYNAMIC_COLOR] = next.useDynamicColor
            prefs[DEFAULT_CURRENCY] = next.defaultCurrency.value
        }
    }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[NOTIFICATION_PERMISSION_REQUESTED] = true }
    }

    private fun Preferences.toSettings(): UserSettings {
        val default = UserSettings.DEFAULT
        val defaultReminder = default.defaultReminder
        val daysBefore = this[REMINDER_DAYS_BEFORE]
            ?.takeIf { it in Reminder.DAYS_BEFORE_OPTIONS }
            ?: defaultReminder.daysBefore
        val time = this[REMINDER_MINUTE_OF_DAY]
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime.of(it / 60, it % 60) }
            ?: defaultReminder.time
        return UserSettings(
            defaultReminder = Reminder(
                enabled = this[REMINDER_ENABLED] ?: defaultReminder.enabled,
                daysBefore = daysBefore,
                time = time
            ),
            themeMode = ThemeMode.entries.firstOrNull { it.name == this[THEME_MODE] }
                ?: default.themeMode,
            useDynamicColor = this[USE_DYNAMIC_COLOR] ?: default.useDynamicColor,
            defaultCurrency =
                CurrencyCode.SUPPORTED.firstOrNull { it.value == this[DEFAULT_CURRENCY] }
                    ?: default.defaultCurrency
        )
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
        val REMINDER_ENABLED = booleanPreferencesKey("default_reminder_enabled")
        val REMINDER_DAYS_BEFORE = intPreferencesKey("default_reminder_days_before")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("default_reminder_minute_of_day")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val NOTIFICATION_PERMISSION_REQUESTED =
            booleanPreferencesKey("notification_permission_requested")
    }
}
