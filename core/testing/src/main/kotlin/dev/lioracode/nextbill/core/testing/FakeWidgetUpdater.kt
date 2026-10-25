package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.effect.WidgetUpdater
import java.util.concurrent.atomic.AtomicInteger

class FakeWidgetUpdater(@Volatile var failWith: Throwable? = null) : WidgetUpdater {
    private val counter = AtomicInteger()
    val updateCount: Int get() = counter.get()

    override suspend fun updateAll() {
        counter.incrementAndGet()
        failWith?.let { throw it }
    }
}
