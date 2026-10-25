package dev.lioracode.nextbill.feature.home.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbPill
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.model.format
import dev.lioracode.nextbill.core.ui.MoneyText
import dev.lioracode.nextbill.feature.home.impl.HomeUiState
import dev.lioracode.nextbill.feature.home.impl.R

@Composable
internal fun SummaryCard(state: HomeUiState.Ready, modifier: Modifier = Modifier) {
    val colors = NextBillTheme.colors
    NbCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.home_due_this_month),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            NbPill(stringArrayResource(R.array.home_months)[state.month.monthValue - 1])
        }
        MoneyText(
            money = state.due.primary,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 8.dp),
            color = if (state.due.primary.minor == 0L) colors.textTertiary else colors.textPrimary
        )
        if (state.due.others.isNotEmpty()) {
            Text(
                text = state.due.others.joinToString(" ") { "+ ${it.format()}" },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.borderSubtle
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = fixedCostLine(state.fixedCost, colors.textPrimary),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

/** "월 고정비 ₩72,300"에서 금액만 semibold textPrimary로 강조합니다. */
@Composable
private fun fixedCostLine(summary: AmountSummary, amountColor: Color): AnnotatedString {
    val amounts = (listOf(summary.primary) + summary.others).joinToString(" · ") { it.format() }
    val full = stringResource(R.string.home_fixed_cost, amounts)
    val start = full.indexOf(amounts)
    return buildAnnotatedString {
        append(full)
        if (start >= 0) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold, color = amountColor),
                start,
                start + amounts.length
            )
        }
    }
}
