package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.domain.repository.ScheduledReminderRepository
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class FakeScheduledReminderRepository(initial: Collection<PlannedReminder> = emptyList()) :
    ScheduledReminderRepository {
    private val state = MutableStateFlow(initial.associateBy { it.subscriptionId })

    /** 호출마다 가상 시간으로 이만큼 지연합니다. */
    @Volatile var callDelay: Duration = Duration.ZERO

    @Volatile var failNextPut: Throwable? = null

    @Volatile var failNextRemove: Throwable? = null

    @Volatile var failNextRemoveIfMatches: Throwable? = null

    private val getAllCalls = AtomicInteger()
    private val putCalls = AtomicInteger()
    private val removeCalls = AtomicInteger()
    private val removeIfMatchesCalls = AtomicInteger()

    val getAllCount: Int get() = getAllCalls.get()
    val putCount: Int get() = putCalls.get()
    val removeCount: Int get() = removeCalls.get()
    val removeIfMatchesCount: Int get() = removeIfMatchesCalls.get()

    /** 현재 기록(테스트 확인용, 호출 횟수에 안 잡힘). */
    val snapshot: Map<SubscriptionId, PlannedReminder> get() = state.value

    override suspend fun getAll(): Map<SubscriptionId, PlannedReminder> {
        pause()
        getAllCalls.incrementAndGet()
        return state.value
    }

    override suspend fun put(r: PlannedReminder) {
        pause()
        putCalls.incrementAndGet()
        failNextPut?.let {
            failNextPut = null
            throw it
        }
        synchronized(state) { state.value = state.value + (r.subscriptionId to r) }
    }

    override suspend fun remove(id: SubscriptionId) {
        pause()
        removeCalls.incrementAndGet()
        failNextRemove?.let {
            failNextRemove = null
            throw it
        }
        synchronized(state) { state.value = state.value - id }
    }

    override suspend fun removeIfMatches(rs: Collection<PlannedReminder>): Int {
        pause()
        removeIfMatchesCalls.incrementAndGet()
        failNextRemoveIfMatches?.let {
            failNextRemoveIfMatches = null
            throw it
        }
        return synchronized(state) {
            val matched = rs.filter { state.value[it.subscriptionId] == it }
            state.value = state.value - matched.map { it.subscriptionId }.toSet()
            matched.map { it.subscriptionId }.toSet().size
        }
    }

    private suspend fun pause() {
        val d = callDelay
        if (d > Duration.ZERO) delay(d)
    }
}
