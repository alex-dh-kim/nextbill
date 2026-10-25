package dev.lioracode.nextbill.feature.home.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.ui.DueChip
import dev.lioracode.nextbill.core.ui.LetterAvatar
import dev.lioracode.nextbill.core.ui.MoneyText
import dev.lioracode.nextbill.core.ui.cycleText
import dev.lioracode.nextbill.feature.home.impl.UpcomingItem

@Composable
internal fun UpcomingRow(
    item: UpcomingItem,
    shape: Shape,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val borderColor = colors.borderSubtle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceCard)
            .clickable(onClick = onClick)
            .drawWithContent {
                drawContent()
                // 한 카드처럼 보이도록 첫 행이 아니면 위 테두리를 그리지 않습니다(위 행의 아래 테두리가 구분선).
                // 모양으로 잘리므로 2배 굵기로 그리면 안쪽 1dp만 남습니다.
                val width = 1.dp.toPx()
                clipRect(top = if (isFirst) 0f else width) {
                    drawOutline(
                        shape.createOutline(size, layoutDirection, this@drawWithContent),
                        color = borderColor,
                        style = Stroke(width = width * 2)
                    )
                }
                if (!isFirst) {
                    // 위 테두리를 잘라낸 자리에서 좌우 테두리의 맨 윗 1dp가 끊기지 않게 채웁니다.
                    drawRect(borderColor, Offset.Zero, Size(width, width))
                    drawRect(borderColor, Offset(size.width - width, 0f), Size(width, width))
                }
            }
            .defaultMinSize(minHeight = 72.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LetterAvatar(item.name, item.colorKey, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                cycleText(item.cycle, item.anchor),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MoneyText(
                item.price,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            DueChip(item.label)
        }
    }
}
