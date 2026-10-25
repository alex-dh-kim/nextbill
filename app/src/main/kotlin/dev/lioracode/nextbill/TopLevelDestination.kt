package dev.lioracode.nextbill

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.feature.calendar.api.CalendarNavKey
import dev.lioracode.nextbill.feature.home.api.HomeNavKey
import dev.lioracode.nextbill.feature.settings.api.SettingsNavKey

/** 하단 탭. 순서가 하단 바의 순서이고, 첫 항목이 시작 탭입니다. */
enum class TopLevelDestination(
    val key: NavKey,
    @DrawableRes val icon: Int,
    @StringRes val label: Int
) {
    HOME(HomeNavKey, NbIcons.Home, R.string.tab_home),
    CALENDAR(CalendarNavKey, NbIcons.CalendarMonth, R.string.tab_calendar),
    SETTINGS(SettingsNavKey, NbIcons.Settings, R.string.tab_settings)
}
