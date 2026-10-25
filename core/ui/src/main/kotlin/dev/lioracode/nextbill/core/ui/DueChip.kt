package dev.lioracode.nextbill.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.lioracode.nextbill.core.designsystem.component.NbPill
import dev.lioracode.nextbill.core.designsystem.component.PillStyle
import dev.lioracode.nextbill.core.domain.reminder.DueLabel

/** Today/Tomorrow는 Urgent, InDays는 Neutral. 색이 아니라 글자("오늘"/"내일"/"D-5")로도 구분됩니다. */
@Composable
fun DueChip(label: DueLabel, modifier: Modifier = Modifier) {
    when (label) {
        DueLabel.Today -> NbPill(
            stringResource(R.string.due_today),
            modifier,
            style = PillStyle.Urgent
        )

        DueLabel.Tomorrow -> NbPill(
            stringResource(R.string.due_tomorrow),
            modifier,
            style = PillStyle.Urgent
        )

        is DueLabel.InDays ->
            NbPill(
                stringResource(R.string.due_in_days, label.days),
                modifier,
                style = PillStyle.Neutral
            )
    }
}
