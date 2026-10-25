package dev.lioracode.nextbill.feature.calendar.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.format
import dev.lioracode.nextbill.core.ui.toColor
import dev.lioracode.nextbill.feature.calendar.impl.CalendarUiState
import dev.lioracode.nextbill.feature.calendar.impl.R
import dev.lioracode.nextbill.feature.calendar.impl.monthCells
import java.time.DayOfWeek
import java.time.LocalDate

private val CellHeight = 48.dp

@Composable
internal fun MonthCard(
    state: CalendarUiState,
    horizontalPadding: Dp,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val months = stringArrayResource(R.array.calendar_months)
    NbCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    painterResource(NbIcons.ChevronLeft),
                    stringResource(R.string.calendar_prev_month),
                    tint = colors.textSecondary
                )
            }
            Text(
                stringResource(
                    R.string.calendar_month_title,
                    months[state.month.monthValue - 1],
                    state.month.year
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    painterResource(NbIcons.ChevronRight),
                    stringResource(R.string.calendar_next_month),
                    tint = colors.textSecondary
                )
            }
        }
        MonthGrid(state, onSelect)
    }
}

@Composable
internal fun MonthGrid(
    state: CalendarUiState,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = stringArrayResource(R.array.calendar_weekdays)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).map { state.firstDayOfWeek.plus(it.toLong()) }.forEach { day ->
                Text(
                    weekdays[day.value - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = weekdayColor(day),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells = monthCells(state.month, state.firstDayOfWeek)
        val leading = cells.indexOfFirst { it != null }
        monthCells(state.month, state.firstDayOfWeek).chunked(7).forEachIndexed { w, week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEachIndexed { i, date ->
                    if (date == null) {
                        // 이웃 달의 날짜를 흐리게 보여 줍니다. 누를 수 없고 접근성에도 노출하지 않습니다.
                        val index = w * 7 + i
                        FadedCell(state.month.atDay(1).plusDays((index - leading).toLong()))
                    } else {
                        DayCell(date, state, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.FadedCell(date: LocalDate) {
    Box(
        modifier = Modifier.weight(1f).height(CellHeight).clearAndSetSemantics {},
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = NextBillTheme.colors.textTertiary.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun weekdayColor(day: DayOfWeek) = when (day) {
    DayOfWeek.SUNDAY -> NextBillTheme.colors.urgentText
    DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.primary
    else -> NextBillTheme.colors.textSecondary
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    state: CalendarUiState,
    onSelect: (LocalDate) -> Unit
) {
    val colors = NextBillTheme.colors
    val isSelected = date == state.selected
    val isToday = date == state.today
    val count = state.paymentCounts[date] ?: 0
    val months = stringArrayResource(R.array.calendar_months)
    val description = buildString {
        append(
            stringResource(R.string.calendar_day_desc, months[date.monthValue - 1], date.dayOfMonth)
        )
        if (count >
            0
        ) {
            append(pluralStringResource(R.plurals.calendar_day_desc_payments, count, count))
        }
        if (isSelected) append(stringResource(R.string.calendar_day_desc_selected))
    }
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .weight(1f)
            .height(CellHeight)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = { onSelect(date) })
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .then(
                    if (isToday && isSelected) {
                        Modifier.border(1.5.dp, primary.copy(alpha = 0.3f), CircleShape)
                    } else {
                        Modifier
                    }
                )
                .padding(2.dp)
                .clip(CircleShape)
                .then(if (isSelected) Modifier.background(primary) else Modifier)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(1.5.dp, primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary

                    else -> when (date.dayOfWeek) {
                        DayOfWeek.SUNDAY -> colors.urgentText
                        DayOfWeek.SATURDAY -> primary
                        else -> colors.textPrimary
                    }
                }
            )
        }
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            state.dots[date].orEmpty().forEach {
                Box(Modifier.size(6.dp).background(it.toColor(), CircleShape))
            }
        }
    }
}

@Composable
internal fun MonthSummary(state: CalendarUiState, modifier: Modifier = Modifier) {
    val colors = NextBillTheme.colors
    val amounts = (listOf(state.monthTotal.primary) + state.monthTotal.others)
        .joinToString(" · ") { it.format() }
    NbCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.badgeNeutralBg.copy(alpha = 0.6f))
            ) {
                Icon(
                    painterResource(NbIcons.ReceiptLong),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    stringResource(R.string.calendar_month_summary_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Text(
                    pluralStringResource(
                        R.plurals.calendar_month_summary,
                        state.monthCount,
                        state.monthCount,
                        amounts
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            }
        }
    }
}
