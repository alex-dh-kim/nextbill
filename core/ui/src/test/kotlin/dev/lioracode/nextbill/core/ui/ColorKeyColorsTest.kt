package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.ColorKey
import org.junit.Test

class ColorKeyColorsTest {
    @Test fun hexTable() {
        val expected = mapOf(
            ColorKey.RED to Color(0xFFE50914),
            ColorKey.BLUE to Color(0xFF1B64DA),
            ColorKey.GREEN to Color(0xFF00A884),
            ColorKey.AMBER to Color(0xFFF59E0B),
            ColorKey.VIOLET to Color(0xFF7C3AED),
            ColorKey.PINK to Color(0xFFEC4899),
            ColorKey.SLATE to Color(0xFF4E5968),
            ColorKey.INK to Color(0xFF191F28)
        )
        assertThat(ColorKey.entries.toSet()).isEqualTo(expected.keys)
        expected.forEach { (key, color) -> assertThat(key.toColor()).isEqualTo(color) }
    }
}
