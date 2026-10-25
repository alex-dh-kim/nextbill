package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.feature.editor.impl.R
import java.util.Currency
import java.util.Locale

/** 금액 입력(시안 02 두 번째 카드). 오류가 있으면 카드 테두리·숫자·문구를 error 색으로 그립니다. */
@Composable
internal fun AmountField(
    amountText: String,
    currency: CurrencyCode,
    error: FieldError?,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (CurrencyCode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = NextBillTheme.colors
    val isError = error != null
    val numberColor = if (isError) MaterialTheme.colorScheme.error else colors.textPrimary
    val style = MaterialTheme.typography.displaySmall.copy(color = numberColor)
    val label = stringResource(R.string.editor_amount_label)
    val errorText = error?.let { stringResource(it.messageRes()) }
    EditorCard(modifier = modifier, isError = isError, padding = 24.dp, verticalSpacing = 4.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label, Modifier.weight(1f))
            CurrencyMenu(currency, enabled, onCurrencyChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currency.symbol(), style = style)
            Box(Modifier.padding(start = 4.dp).weight(1f)) {
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    enabled = enabled,
                    textStyle = style,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = label
                            if (errorText != null) error(errorText)
                        },
                    decorationBox = { inner ->
                        if (amountText.isEmpty()) {
                            Text(
                                "0",
                                style = style.copy(
                                    color = if (isError) {
                                        numberColor
                                    } else {
                                        colors.textTertiary
                                    }
                                )
                            )
                        }
                        inner()
                    }
                )
            }
        }
        if (error != null) FieldErrorText(error, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CurrencyMenu(
    currency: CurrencyCode,
    enabled: Boolean,
    onSelect: (CurrencyCode) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val colors = NextBillTheme.colors
    Box(
        modifier = Modifier.heightIn(min = MinTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        val description = stringResource(R.string.editor_currency_desc, currency.value)
        Surface(
            onClick = { open = true },
            enabled = enabled,
            shape = CircleShape,
            color = colors.surfaceCanvas,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.semantics { contentDescription = description }
        ) {
            Row(
                modifier = Modifier.padding(
                    PaddingValues(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    currency.value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Icon(
                    painterResource(R.drawable.editor_ic_arrow_drop_down),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CurrencyCode.SUPPORTED.forEach { code ->
                DropdownMenuItem(
                    text = { Text("${code.value}  ${code.symbol()}") },
                    onClick = {
                        open = false
                        onSelect(code)
                    }
                )
            }
        }
    }
}

/** 표시용 기호. [dev.lioracode.nextbill.core.model.format]과 같이 `Locale.US` 기준(₩, $, €, ¥). */
private fun CurrencyCode.symbol(): String = Currency.getInstance(value).getSymbol(Locale.US)
