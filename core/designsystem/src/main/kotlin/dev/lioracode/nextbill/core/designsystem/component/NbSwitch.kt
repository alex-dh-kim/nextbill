package dev.lioracode.nextbill.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

/** M3 Switch는 내부에서 48dp 최소 터치 영역을 보장합니다. */
@Composable
fun NbSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            uncheckedTrackColor = NextBillTheme.colors.badgeNeutralBg,
            uncheckedThumbColor = NextBillTheme.colors.surfaceCard,
            uncheckedBorderColor = NextBillTheme.colors.borderSubtle
        )
    )
}
