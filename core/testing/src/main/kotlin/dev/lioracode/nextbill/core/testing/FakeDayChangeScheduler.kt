package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import java.util.concurrent.atomic.AtomicInteger

class FakeDayChangeScheduler : DayChangeScheduler {
    private val counter = AtomicInteger()
    val count: Int get() = counter.get()

    override suspend fun scheduleNext() {
        counter.incrementAndGet()
    }
}
