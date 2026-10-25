package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.effect.ReminderAlarm
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * 등록된 OS 알람은 항상 0~1개입니다([nextAt]).
 *
 * [cancel]은 suspend가 아니므로 [cancelGate]는 호출한 스레드를 막습니다(runBlocking).
 * 막힌 스레드가 테스트 디스패처의 유일한 스레드이면 풀어 줄 수 없으니, 막힐 호출은
 * `Dispatchers.Default` 등 다른 스레드에서 실행하고 [cancelEntered]로 도착을 기다린 뒤
 * `cancelGate.complete(Unit)`으로 풉니다.
 */
class FakeReminderAlarm(
    @Volatile var nextAt: Instant? = null,
    @Volatile var setCount: Int = 0,
    @Volatile var failNext: Throwable? = null,
    @Volatile var failNextCancel: Throwable? = null
) : ReminderAlarm {
    @Volatile var cancelCount: Int = 0

    @Volatile var cancelGate: CompletableDeferred<Unit>? = null

    /** [cancel]이 게이트에 도착하면 완료됩니다. 한 번만 완료되니 다시 쓰려면 [resetEntered]를 부릅니다. */
    @Volatile var cancelEntered: CompletableDeferred<Unit> = CompletableDeferred()

    fun resetEntered() {
        cancelEntered = CompletableDeferred()
    }

    @Synchronized
    override fun setNext(at: Instant) {
        failNext?.let {
            failNext = null
            throw it
        }
        nextAt = at
        setCount++
    }

    override fun cancel() {
        cancelGate?.let { gate ->
            cancelEntered.complete(Unit)
            runBlocking { withTimeout(GATE_TIMEOUT) { gate.await() } }
        }
        synchronized(this) {
            failNextCancel?.let {
                failNextCancel = null
                throw it
            }
            nextAt = null
            cancelCount++
        }
    }
}
