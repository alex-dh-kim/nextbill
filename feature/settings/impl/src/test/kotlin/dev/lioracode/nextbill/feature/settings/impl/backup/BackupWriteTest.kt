package dev.lioracode.nextbill.feature.settings.impl.backup

import android.app.Application
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupWriteTest {
    @get:Rule val folder = TemporaryFolder()

    private val resolver: ContentResolver
        get() = ApplicationProvider.getApplicationContext<Application>().contentResolver

    private val oldLonger = """{"schemaVersion":1,"subscriptions":[""" + "x".repeat(500) + "]}"
    private val newShorter = """{"schemaVersion":1,"subscriptions":[]}"""

    private fun provider(file: File, rejectTruncate: Boolean = false): LiteralModeProvider =
        Robolectric.setupContentProvider(LiteralModeProvider::class.java, AUTHORITY).also {
            it.file = file
            it.rejectTruncate = rejectTruncate
        }

    private val uri = Uri.parse("content://$AUTHORITY/backup.json")

    @Test
    fun overwritingLongerFileLeavesExactlyNewContent() {
        // 같은 날 다시 내보내 더 긴 기존 파일 위에 씁니다.
        val file = folder.newFile("nextbill-backup-2026-10-26.json").apply { writeText(oldLonger) }
        val provider = provider(file)

        resolver.writeBackup(uri, newShorter)

        assertThat(provider.modes).containsExactly("wt")
        assertThat(file.readText()).isEqualTo(newShorter)
        assertThat(resolver.readBackup(uri)).isEqualTo(newShorter)
    }

    @Test
    fun providerRejectingTruncateFallsBackToWrite() {
        val file = folder.newFile("backup.json")
        val provider = provider(file, rejectTruncate = true)

        resolver.writeBackup(uri, newShorter)

        assertThat(provider.modes).containsExactly("wt", "w").inOrder()
        assertThat(file.readText()).isEqualTo(newShorter)
    }

    /** 제공자의 IllegalArgumentException·IllegalStateException은 IOException으로 바뀝니다(화면은 IoFailed). */
    @Test
    fun providerRuntimeFailuresBecomeIoExceptions() {
        val provider = provider(folder.newFile("backup.json"))
        listOf(IllegalArgumentException("bad uri"), IllegalStateException("gone")).forEach { e ->
            provider.failWith = e
            val write = runCatching { resolver.writeBackup(uri, newShorter) }.exceptionOrNull()
            assertThat(write).isInstanceOf(IOException::class.java)
            assertThat(write?.cause).isSameInstanceAs(e)
            val read = runCatching { resolver.readBackup(uri) }.exceptionOrNull()
            assertThat(read).isInstanceOf(IOException::class.java)
        }
    }

    /**
     * 모드를 글자 그대로 따르는 문서 제공자: "w"는 자르지 않고(Android 10+ 일부 제공자처럼), "wt"만 자릅니다.
     * [rejectTruncate]면 "wt"를 거부합니다.
     */
    class LiteralModeProvider : ContentProvider() {
        lateinit var file: File
        var rejectTruncate = false
        var failWith: RuntimeException? = null
        val modes = mutableListOf<String>()

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            modes += mode
            failWith?.let { throw it }
            val base = ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE
            val flags = when (mode) {
                "w" -> base

                "wt" -> if (rejectTruncate) {
                    throw FileNotFoundException("unsupported mode $mode")
                } else {
                    base or ParcelFileDescriptor.MODE_TRUNCATE
                }

                "r" -> ParcelFileDescriptor.MODE_READ_ONLY

                else -> throw FileNotFoundException("unsupported mode $mode")
            }
            return ParcelFileDescriptor.open(file, flags)
        }

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? = null

        override fun getType(uri: Uri): String = "application/json"

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ) = 0
    }

    private companion object {
        const val AUTHORITY = "dev.lioracode.nextbill.test.backup"
    }
}
