package dev.lioracode.nextbill.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NbTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes navigationIcon: Int? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(painterResource(navigationIcon), navigationContentDescription)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NextBillTheme.colors.surfaceCanvas,
            titleContentColor = NextBillTheme.colors.textPrimary,
            navigationIconContentColor = NextBillTheme.colors.textPrimary,
            actionIconContentColor = NextBillTheme.colors.textPrimary
        )
    )
}
