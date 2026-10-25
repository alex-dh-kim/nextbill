package dev.lioracode.nextbill.core.domain.backup

import dev.lioracode.nextbill.core.model.Outcome

/** 백업 파일 크기 한도(문자 수). 한도 안 최대 상태의 encode 길이보다 큽니다. */
const val MAX_BACKUP_CHARS = 8_000_000

sealed interface BackupError {
    data object Malformed : BackupError

    data object TooLarge : BackupError

    data class UnsupportedVersion(val found: Int) : BackupError
}

interface BackupCodec {
    fun encode(snapshot: BackupSnapshot): String

    fun decode(json: String): Outcome<BackupError, BackupSnapshot>
}
