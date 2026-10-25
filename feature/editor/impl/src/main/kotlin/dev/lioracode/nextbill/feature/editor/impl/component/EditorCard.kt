package dev.lioracode.nextbill.feature.editor.impl.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.feature.editor.impl.R

/** 시안 02의 섹션 카드: 흰 배경, 16dp 모서리, 1dp 테두리(오류면 error 색). */
@Composable
internal fun EditorCard(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    padding: Dp = 16.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = NbShapes.Card,
        color = NextBillTheme.colors.surfaceCard,
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error else NextBillTheme.colors.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}

/** 카드 안의 작은 제목(label-md, medium). */
@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = NextBillTheme.colors.textSecondary
    )
}

/** 입력 오류: 아이콘 + 문구(error 색). */
@Composable
internal fun FieldErrorText(error: FieldError, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painterResource(R.drawable.editor_ic_error),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

internal fun FieldError.messageRes(): Int = when (this) {
    FieldError.NAME_BLANK -> R.string.editor_error_name_blank
    FieldError.NAME_TOO_LONG -> R.string.editor_error_name_too_long
    FieldError.AMOUNT_BLANK -> R.string.editor_error_amount_blank
    FieldError.AMOUNT_INVALID -> R.string.editor_error_amount_invalid
    FieldError.AMOUNT_TOO_LARGE -> R.string.editor_error_amount_too_large
    FieldError.INTERVAL_INVALID -> R.string.editor_error_interval_invalid
    FieldError.MEMO_TOO_LONG -> R.string.editor_error_memo_too_long
    FieldError.ANCHOR_OUT_OF_RANGE -> R.string.editor_error_anchor_out_of_range
}
