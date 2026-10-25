package dev.lioracode.nextbill.core.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DateTextTest {
    @get:Rule
    val rule = createComposeRule()

    private fun text(): String {
        var out = ""
        rule.setContent { out = shortDateText(LocalDate.of(2026, 11, 15)) }
        rule.waitForIdle()
        return out
    }

    @Test
    @Config(qualifiers = "ko")
    fun ko() = assertThat(text()).isEqualTo("11월 15일 (일)")

    @Test
    @Config(qualifiers = "en")
    fun en() = assertThat(text()).isEqualTo("Sun, Nov 15")
}
