package dev.lioracode.nextbill.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.ui.shortDateText
import dev.lioracode.nextbill.feature.calendar.impl.component.DayPaymentRow
import dev.lioracode.nextbill.feature.calendar.impl.component.MonthCard
import dev.lioracode.nextbill.feature.calendar.impl.component.MonthSummary
import java.time.LocalDate

private val NarrowWidth = 376.dp
private val NarrowMargin = 12.dp
private val CellSize = 48.dp
private val CardInnerPadding = 16.dp

@Composable
fun CalendarScreen(
    state: CalendarUiState?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onPaymentClick: (SubscriptionId) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    Column(modifier = modifier.fillMaxSize()) {
        NbTopAppBar(
            title = stringResource(R.string.calendar_title),
            titleStyle = MaterialTheme.typography.headlineMedium,
            actions = {
                TodayButton(onToday, Modifier.padding(end = NextBillTheme.spacing.margin))
            }
        )
        if (state == null) return@Column
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // 360dp 창에서 칸 폭 48dp(360 - 24 = 336 = 7 x 48)를 확보하려고 좁은 창은 여백을 줄입니다.
            val margin = if (maxWidth < NarrowWidth) NarrowMargin else NextBillTheme.spacing.margin
            val roomy = (maxWidth - margin * 2 - CardInnerPadding * 2) / 7 >= CellSize
            val inner: Dp = if (roomy) CardInnerPadding else 0.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = margin,
                    end = margin,
                    top = NextBillTheme.spacing.sm,
                    bottom = NextBillTheme.spacing.xl
                ),
                verticalArrangement = Arrangement.spacedBy(NextBillTheme.spacing.sm)
            ) {
                item(key = "month") {
                    MonthCard(
                        state = state,
                        horizontalPadding = inner,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onSelect = onSelect
                    )
                }
                item(key = "summary") {
                    MonthSummary(state, Modifier.padding(bottom = NextBillTheme.spacing.sm))
                }
                item(key = "title") {
                    Text(
                        shortDateText(state.selected),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                if (state.selectedPayments.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(R.string.calendar_no_payments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(state.selectedPayments, key = { it.id.value }) {
                        DayPaymentRow(it, isToday = state.selected == state.today, onClick = {
                            onPaymentClick(it.id)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NextBillTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.badgeNeutralBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painterResource(NbIcons.CalendarMonth),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            stringResource(R.string.calendar_today),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
    }
}
