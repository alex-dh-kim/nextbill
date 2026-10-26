package dev.lioracode.nextbill.feature.settings.impl.backup

import java.io.IOException
import java.io.Reader

/** 백업 파일이 한도(`MAX_BACKUP_CHARS`)를 넘어 읽기를 멈췄습니다. */
class BackupTooLargeException : IOException("Backup file exceeds the size limit")

internal const val BUFFER_CHARS = 8 * 1024

/**
 * [maxChars]자까지 읽습니다. 한도 + 1자를 확인하는 순간 더 읽지 않고 [BackupTooLargeException]을 던집니다.
 * 큰 파일 전체를 메모리에 올리지 않기 위해서입니다.
 */
internal fun Reader.readAtMost(maxChars: Int): String {
    val out = StringBuilder()
    val buffer = CharArray(BUFFER_CHARS)
    while (true) {
        val want = minOf(buffer.size, maxChars + 1 - out.length)
        val n = read(buffer, 0, want)
        if (n < 0) return out.toString()
        out.append(buffer, 0, n)
        if (out.length > maxChars) throw BackupTooLargeException()
    }
}
