package dev.lioracode.nextbill.feature.settings.impl.backup

import com.google.common.truth.Truth.assertThat
import java.io.Reader
import java.io.StringReader
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupReadTest {
    @Test
    fun readsWholeTextWithinLimit() {
        assertThat(StringReader("abcde").readAtMost(5)).isEqualTo("abcde")
        assertThat(StringReader("").readAtMost(5)).isEmpty()
    }

    @Test
    fun overLimitStopsReadingAndThrows() {
        val reader = CountingReader(StringReader("x".repeat(100_000)))
        assertThrows(BackupTooLargeException::class.java) { reader.readAtMost(10) }
        // 한도 + 1자를 확인하면 멈춥니다(버퍼 하나 이상 읽지 않음).
        assertThat(reader.read).isAtMost(10 + 1 + BUFFER_CHARS)
    }

    private class CountingReader(private val inner: Reader) : Reader() {
        var read = 0

        override fun read(cbuf: CharArray, off: Int, len: Int): Int =
            inner.read(cbuf, off, len).also { if (it > 0) read += it }

        override fun close() = inner.close()
    }
}
