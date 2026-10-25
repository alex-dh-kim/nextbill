package dev.lioracode.nextbill.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate

/** ko "11월 15일 (일)", en "Sun, Nov 15". */
@Composable
fun shortDateText(date: LocalDate): String = stringResource(
    R.string.date_short,
    stringArrayResource(R.array.month_short)[date.monthValue - 1],
    date.dayOfMonth,
    stringArrayResource(R.array.weekday_short)[date.dayOfWeek.value - 1]
)

/** ko "3월 2일", en "Mar 2". */
@Composable
internal fun monthDayText(date: LocalDate): String = stringResource(
    R.string.date_month_day,
    stringArrayResource(R.array.month_short)[date.monthValue - 1],
    date.dayOfMonth
)

@Composable
internal fun weekdayFullText(date: LocalDate): String =
    stringArrayResource(R.array.weekday_full)[date.dayOfWeek.value - 1]
