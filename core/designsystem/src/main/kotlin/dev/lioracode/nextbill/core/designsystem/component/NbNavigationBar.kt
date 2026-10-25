package dev.lioracode.nextbill.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

data class NbNavItem(val label: String, @DrawableRes val icon: Int)

/** 하단 탭 바. 선택 항목은 primary, 나머지는 textTertiary. 항목은 M3가 48dp 이상으로 보장합니다. */
@Composable
fun NbNavigationBar(
    items: List<NbNavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    NavigationBar(
        modifier = modifier.drawBehind {
            drawLine(
                colors.borderSubtle,
                Offset.Zero,
                Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        },
        containerColor = colors.surfaceCard,
        tonalElevation = Dp.Hairline
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selected,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textTertiary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
