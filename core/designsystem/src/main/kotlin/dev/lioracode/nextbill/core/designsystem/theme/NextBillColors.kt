package dev.lioracode.nextbill.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** M3 ColorScheme에 없는 의미 토큰까지 묶은 색 모음(시안 DESIGN.md의 이름). */
data class NextBillColors(
    val surfaceCanvas: Color,
    val surfaceCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: Color,
    val urgentSurface: Color,
    val urgentText: Color,
    val badgeNeutralBg: Color,
    val badgeNeutralText: Color
)

internal val LightNextBillColors = NextBillColors(
    surfaceCanvas = Color(0xFFF2F4F6),
    surfaceCard = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF191F28),
    textSecondary = Color(0xFF6B7684),
    textTertiary = Color(0xFF8B95A1),
    borderSubtle = Color(0xFFE5E8EB),
    urgentSurface = Color(0xFFFFF1F2),
    urgentText = Color(0xFFBE123C),
    badgeNeutralBg = Color(0xFFEAECEF),
    badgeNeutralText = Color(0xFF4E5968)
)

internal val DarkNextBillColors = NextBillColors(
    surfaceCanvas = Color(0xFF121316),
    surfaceCard = Color(0xFF1C1E22),
    textPrimary = Color(0xFFF8F9FA),
    textSecondary = Color(0xFF9AA4B2),
    textTertiary = Color(0xFF6B7280),
    borderSubtle = Color(0xFF282A2F),
    urgentSurface = Color(0xFF3D141A),
    urgentText = Color(0xFFFF4D6D),
    badgeNeutralBg = Color(0xFF282C35),
    badgeNeutralText = Color(0xFF9AA4B2)
)

/** 동적 색상(Material You)일 때 ColorScheme에서 파생합니다. */
internal fun ColorScheme.toNextBillColors() = NextBillColors(
    surfaceCanvas = surfaceContainerLow,
    surfaceCard = surface,
    textPrimary = onSurface,
    textSecondary = onSurfaceVariant,
    textTertiary = outline,
    borderSubtle = outlineVariant,
    urgentSurface = errorContainer,
    urgentText = onErrorContainer,
    badgeNeutralBg = surfaceContainerHighest,
    badgeNeutralText = onSurfaceVariant
)
