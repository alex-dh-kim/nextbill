package dev.lioracode.nextbill.core.domain.effect

/**
 * 격리해 삼킨 실패를 밖에 알립니다(Android에서는 기기 로그). 알려도 흐름은 바뀌지 않습니다.
 * `NoSuchMethodError` 같은 Error도 그대로 넘기므로, 기기에서만 나는 실패가 조용히 사라지지 않습니다.
 */
fun interface FailureReporter {
    /** [where]는 실패한 단계 이름입니다. */
    fun report(where: String, error: Throwable)

    companion object {
        /** 아무것도 하지 않습니다(테스트용). */
        val None = FailureReporter { _, _ -> }
    }
}
