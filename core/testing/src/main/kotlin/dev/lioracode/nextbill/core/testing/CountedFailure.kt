package dev.lioracode.nextbill.core.testing

import kotlin.time.Duration.Companion.seconds

/** 다음 [times]번의 호출을 [error]로 실패시킵니다. 다 쓰면 스스로 풀립니다. */
class CountedFailure {
    @Volatile var error: Throwable? = null
        set(value) {
            field = value
            used = 0
        }

    @Volatile var times: Int = 1

    private var used = 0

    @Synchronized
    fun throwIfArmed() {
        val e = error ?: return
        used++
        if (used >= times) {
            error = null
            times = 1
        }
        throw e
    }
}

/** 게이트를 기다리는 최대 시간. 풀어 주는 것을 잊은 테스트가 매달리지 않고 실패하게 합니다. */
val GATE_TIMEOUT = 10.seconds
