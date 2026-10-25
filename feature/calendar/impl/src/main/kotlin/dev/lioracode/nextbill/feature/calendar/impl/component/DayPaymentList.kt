package dev.lioracode.nextbill.feature.calendar.impl.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.ui.DueChip
import dev.lioracode.nextbill.core.ui.LetterAvatar
import dev.lioracode.nextbill.core.ui.MoneyText
import dev.lioracode.nextbill.core.ui.cycleText
import dev.lioracode.nextbill.feature.calendar.impl.CalendarPayment

@Composable
internal fun DayPaymentRow(
    payment: CalendarPayment,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    NbCard(modifier = modifier.fillMaxWidth().clip(NbShapes.Card).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LetterAvatar(payment.name, payment.colorKey, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        payment.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isToday) DueChip(DueLabel.Today)
                }
                Text(
                    cycleText(payment.cycle, payment.anchor),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            MoneyText(
                payment.price,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }
    }
}
