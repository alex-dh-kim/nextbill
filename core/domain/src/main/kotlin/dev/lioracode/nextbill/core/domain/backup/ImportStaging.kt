package dev.lioracode.nextbill.core.domain.backup

import java.io.IOException

/** [write]·[clear]는 파일 시스템 실패를 [IOException]으로 알립니다(false 반환을 삼키지 않음). */
interface ImportStaging {
    suspend fun write(json: String)

    suspend fun read(): String?

    suspend fun clear()
}
