package dev.lioracode.nextbill.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalNextBillColors = staticCompositionLocalOf { LightNextBillColors }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalNextBillTypography = staticCompositionLocalOf { DefaultNextBillTypography }

@Composable
fun NextBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        useDynamic -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val nextBillColors = when {
        useDynamic -> colorScheme.toNextBillColors()
        darkTheme -> DarkNextBillColors
        else -> LightNextBillColors
    }
    CompositionLocalProvider(
        LocalNextBillColors provides nextBillColors,
        LocalSpacing provides Spacing(),
        LocalNextBillTypography provides DefaultNextBillTypography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NextBillM3Typography,
            shapes = NextBillM3Shapes,
            content = content
        )
    }
}

object NextBillTheme {
    val colors: NextBillColors
        @Composable @ReadOnlyComposable
        get() = LocalNextBillColors.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current

    val typography: NextBillTypography
        @Composable @ReadOnlyComposable
        get() = LocalNextBillTypography.current
}
