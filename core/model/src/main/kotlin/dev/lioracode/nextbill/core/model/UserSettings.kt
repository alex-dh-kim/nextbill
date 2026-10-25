package dev.lioracode.nextbill.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val defaultReminder: Reminder,
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val defaultCurrency: CurrencyCode
) {
    companion object {
        val DEFAULT = UserSettings(Reminder.DEFAULT, ThemeMode.SYSTEM, false, CurrencyCode.KRW)
    }
}
