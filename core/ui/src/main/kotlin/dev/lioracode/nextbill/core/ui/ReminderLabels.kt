package dev.lioracode.nextbill.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.lioracode.nextbill.core.model.Category
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** 카테고리 이름("영상", "Video"). 편집·상세 화면이 함께 씁니다. */
@Composable
fun categoryLabel(category: Category): String = stringResource(
    when (category) {
        Category.VIDEO -> R.string.category_video
        Category.MUSIC -> R.string.category_music
        Category.TELECOM -> R.string.category_telecom
        Category.INSURANCE -> R.string.category_insurance
        Category.CLOUD -> R.string.category_cloud
        Category.LIVING -> R.string.category_living
        Category.OTHER -> R.string.category_other
    }
)

/** "당일" / "1일 전". [days]가 0이면 당일입니다. */
@Composable
fun daysBeforeLabel(days: Int): String = if (days == 0) {
    stringResource(R.string.reminder_same_day)
} else {
    pluralStringResource(R.plurals.reminder_days_before, days, days)
}

/** "오전 9:00" / "9:00 AM". 앱 언어(Configuration)의 짧은 시각 형식입니다. */
@Composable
fun reminderTimeText(time: LocalTime): String {
    val locale = LocalLocale.current.platformLocale
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
}
