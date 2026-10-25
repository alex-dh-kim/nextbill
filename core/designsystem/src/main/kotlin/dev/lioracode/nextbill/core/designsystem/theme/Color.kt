package dev.lioracode.nextbill.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1B64DA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    background = Color(0xFFF2F4F6),
    onBackground = Color(0xFF191F28),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191F28),
    onSurfaceVariant = Color(0xFF6B7684),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFF2F4F6),
    surfaceContainerHigh = Color(0xFFEAECEF),
    surfaceContainerHighest = Color(0xFFEAECEF),
    outline = Color(0xFF8B95A1),
    outlineVariant = Color(0xFFE5E8EB),
    error = Color(0xFFE11D48),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFF1F2),
    onErrorContainer = Color(0xFFBE123C)
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF121316),
    onBackground = Color(0xFFF8F9FA),
    surface = Color(0xFF1C1E22),
    onSurface = Color(0xFFF8F9FA),
    onSurfaceVariant = Color(0xFF9AA4B2),
    surfaceContainerLowest = Color(0xFF121316),
    surfaceContainerLow = Color(0xFF121316),
    surfaceContainer = Color(0xFF1C1E22),
    surfaceContainerHigh = Color(0xFF282C35),
    surfaceContainerHighest = Color(0xFF282C35),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF282A2F),
    error = Color(0xFFFF4D6D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF3D141A),
    onErrorContainer = Color(0xFFFF4D6D)
)
