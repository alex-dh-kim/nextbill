package dev.lioracode.nextbill.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

/** 16dp 둥근 흰 카드. 캔버스 위에서 1dp `borderSubtle` 테두리로 구분합니다. */
@Composable
fun NbCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = NbShapes.Card,
        color = NextBillTheme.colors.surfaceCard,
        border = BorderStroke(1.dp, NextBillTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
