package dev.lioracode.nextbill.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LetterAvatarTest {
    @Test fun korean() = assertThat(avatarLetter("넷플릭스")).isEqualTo("넷")

    @Test fun trimsAndUppercases() = assertThat(avatarLetter(" icloud+")).isEqualTo("I")

    @Test fun emptyAndBlank() {
        assertThat(avatarLetter("")).isEqualTo("?")
        assertThat(avatarLetter("   ")).isEqualTo("?")
    }

    @Test fun emojiKeptWhole() = assertThat(avatarLetter("😀abc")).isEqualTo("😀")

    @Test fun zwjSequenceTakesFirstCodePointOnly() = assertThat(
        avatarLetter("👨‍👩‍👧x")
    ).isEqualTo("👨")
}
