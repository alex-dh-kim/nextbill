package dev.lioracode.nextbill.core.domain.usecase

import kotlin.coroutines.cancellation.CancellationException

/**
 * 실패를 격리하는 [runCatching]. 코루틴 취소([CancellationException])는 삼키지 않고 다시 던집니다.
 * inline이라 suspend 함수도 안에서 부를 수 있습니다.
 */
inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
