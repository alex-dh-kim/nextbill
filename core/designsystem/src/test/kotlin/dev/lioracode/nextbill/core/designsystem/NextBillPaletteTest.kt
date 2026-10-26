package dev.lioracode.nextbill.core.designsystem

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.designsystem.theme.NextBillPalette
import org.junit.Test

class NextBillPaletteTest {
    @Test
    fun lightAndDarkExposeDesignTokens() {
        assertThat(NextBillPalette.scheme(false).primary).isEqualTo(Color(0xFF1B64DA))
        assertThat(NextBillPalette.scheme(true).surface).isEqualTo(Color(0xFF1C1E22))
        assertThat(NextBillPalette.colors(false).urgentText).isEqualTo(Color(0xFFBE123C))
        assertThat(NextBillPalette.colors(true).surfaceCard).isEqualTo(Color(0xFF1C1E22))
    }
}
