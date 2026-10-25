package dev.lioracode.nextbill.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

enum class PillStyle { Neutral, Urgent }

/** D-day·상태 칩. Urgent는 "오늘"/"내일"처럼 임박한 결제에만 씁니다. */
@Composable
fun NbPill(text: String, modifier: Modifier = Modifier, style: PillStyle = PillStyle.Neutral) {
    val colors = NextBillTheme.colors
    val (bg, fg) = when (style) {
        PillStyle.Neutral -> colors.badgeNeutralBg to colors.badgeNeutralText
        PillStyle.Urgent -> colors.urgentSurface to colors.urgentText
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (style == PillStyle.Urgent) FontWeight.Bold else null,
        color = fg,
        modifier = modifier
            .background(bg, NbShapes.Chip)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
