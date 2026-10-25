package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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

    @Test fun avatarLetterMeetsAaContrastOnTintedBackground() {
        // 고정 카드와, 동적 색상처럼 조금 달라진 카드.
        val lightCards = listOf(Color(0xFFFFFFFF), Color(0xFFF7F2FA))
        val darkCards = listOf(Color(0xFF1C1E22), Color(0xFF211F26))
        val cases = lightCards.map { false to it } + darkCards.map { true to it }
        ColorKey.entries.forEach { key ->
            cases.forEach { (dark, card) ->
                val background = key.avatarBackground(dark).compositeOver(card)
                val ratio = contrast(key.avatarLetterColor(dark), background)
                assertThat(ratio).isAtLeast(4.5)
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
