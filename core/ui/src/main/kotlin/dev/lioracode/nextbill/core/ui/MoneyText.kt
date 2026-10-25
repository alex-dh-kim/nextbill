package dev.lioracode.nextbill.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.format

@Composable
fun MoneyText(
    money: Money,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(text = money.format(), style = style, color = color, modifier = modifier)
}
