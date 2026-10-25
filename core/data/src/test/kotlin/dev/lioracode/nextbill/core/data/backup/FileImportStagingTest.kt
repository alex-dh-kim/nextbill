package dev.lioracode.nextbill.core.data.backup

import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileImportStagingTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val staging = FileImportStaging(context, Dispatchers.Unconfined)
    private val target = File(context.filesDir, "import-staging.json")

    @Test
    fun stagingWriteReadClear() = runTest {
        assertThat(staging.read()).isNull()
        staging.write("{\"a\":1}")
        assertThat(staging.read()).isEqualTo("{\"a\":1}")
        staging.write("{\"a\":2}")
        assertThat(staging.read()).isEqualTo("{\"a\":2}")
        staging.clear()
        assertThat(staging.read()).isNull()
        staging.clear() // 이미 없어도 실패하지 않음
    }

    @Test
    fun stagingRenameFailureThrows() = runTest {
        target.delete()
        assertThat(target.mkdirs()).isTrue()
        try {
            assertThrows(IOException::class.java) { runBlocking { staging.write("x") } }
            assertThat(File(context.filesDir, "import-staging.json.tmp").exists()).isFalse()
        } finally {
            target.deleteRecursively()
        }
    }

    @Test
    fun stagingClearFailureThrows() = runTest {
        target.deleteRecursively()
        assertThat(File(target, "child").apply { parentFile!!.mkdirs() }.createNewFile()).isTrue()
        try {
            assertThrows(IOException::class.java) { runBlocking { staging.clear() } }
        } finally {
            target.deleteRecursively()
        }
    }
}
