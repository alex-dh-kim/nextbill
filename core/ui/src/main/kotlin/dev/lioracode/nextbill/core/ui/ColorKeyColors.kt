package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.graphics.Color
import dev.lioracode.nextbill.core.model.ColorKey

fun ColorKey.toColor(): Color = when (this) {
    ColorKey.RED -> Color(0xFFE50914)
    ColorKey.BLUE -> Color(0xFF1B64DA)
    ColorKey.GREEN -> Color(0xFF00A884)
    ColorKey.AMBER -> Color(0xFFF59E0B)
    ColorKey.VIOLET -> Color(0xFF7C3AED)
    ColorKey.PINK -> Color(0xFFEC4899)
    ColorKey.SLATE -> Color(0xFF4E5968)
    ColorKey.INK -> Color(0xFF191F28)
}
