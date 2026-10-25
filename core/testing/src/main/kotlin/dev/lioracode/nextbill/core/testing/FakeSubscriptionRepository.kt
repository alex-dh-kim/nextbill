package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 메모리 구현. 쓰기는 Mutex로 한 번에 하나씩(DB 트랜잭션처럼) 적용하고, 읽기는 막지 않습니다.
 * 목록 정렬: active·all은 id 오름차순, cancelled는 cancelledAt 내림차순(같으면 id 오름차순).
 * [revision]은 처음 0이고 [replaceAll] 성공과 [bumpRevision]마다 1 오릅니다.
 */
class FakeSubscriptionRepository(initial: List<Subscription> = emptyList()) :
    SubscriptionRepository {
    private val state = MutableStateFlow(initial.associateBy { it.id })
    private val writeLock = Mutex()

    @Volatile private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0L) + 1

    @Volatile private var rev = 0L

    /** 호출마다 가상 시간으로 이만큼 지연합니다(읽기·쓰기 모두). */
    @Volatile var callDelay: Duration = Duration.ZERO

    @Volatile var failNextUpsert: Throwable? = null

    @Volatile var failNextDelete: Throwable? = null

    private val replaceAllFailure = CountedFailure()

    /** 다음 [failNextReplaceAllTimes]번의 [replaceAll]을 이 오류로 실패시킵니다(데이터·세대는 그대로). */
    var failNextReplaceAll: Throwable?
        get() = replaceAllFailure.error
        set(value) {
            replaceAllFailure.error = value
        }

    var failNextReplaceAllTimes: Int
        get() = replaceAllFailure.times
        set(value) {
            replaceAllFailure.times = value
        }

    /** 설정하면 [replaceAll]이 적용 직전(락을 쥔 채)에 이 게이트가 열릴 때까지 멈춥니다. */
    @Volatile var replaceAllGate: CompletableDeferred<Unit>? = null

    /** [replaceAll]이 게이트에 도착하면 완료됩니다. 한 번만 완료되니 다시 쓰려면 [resetEntered]를 부릅니다. */
    @Volatile var replaceAllEntered: CompletableDeferred<Unit> = CompletableDeferred()

    fun resetEntered() {
        replaceAllEntered = CompletableDeferred()
    }

    /** 설정하면 다음 [revision] 한 번을 이 오류로 실패시킵니다. */
    @Volatile var failNextRevision: Throwable? = null

    private val getCalls = AtomicInteger()
    private val getAllCalls = AtomicInteger()

    /** [get] 호출 수(실패·지연 전, 호출 시점에 셈). */
    val getCount: Int get() = getCalls.get()

    /** [getAll] 호출 수(호출 시점에 셈). */
    val getAllCount: Int get() = getAllCalls.get()

    @Volatile var upsertCount: Int = 0

    @Volatile var deleteCount: Int = 0

    @Volatile var replaceAllCount: Int = 0

    private fun sortedActive(m: Map<SubscriptionId, Subscription>) =
        m.values.filter { it.status == SubscriptionStatus.ACTIVE }.sortedBy { it.id.value }

    private fun sortedCancelled(m: Map<SubscriptionId, Subscription>) =
        m.values.filter { it.status == SubscriptionStatus.CANCELLED }
            .sortedWith(compareByDescending<Subscription> { it.cancelledAt }.thenBy { it.id.value })

    override fun observeActive(): Flow<List<Subscription>> = state.map { sortedActive(it) }

    override fun observeCancelled(): Flow<List<Subscription>> = state.map { sortedCancelled(it) }

    override fun observeAll(): Flow<List<Subscription>> =
        state.map { m -> m.values.sortedBy { it.id.value } }

    override fun observe(id: SubscriptionId): Flow<Subscription?> = state.map { it[id] }

    override suspend fun get(id: SubscriptionId): Subscription? {
        getCalls.incrementAndGet()
        pause()
        return state.value[id]
    }

    override suspend fun getAll(): List<Subscription> {
        getAllCalls.incrementAndGet()
        pause()
        return state.value.values.sortedBy { it.id.value }
    }

    override suspend fun count(): Int {
        pause()
        return state.value.size
    }

    override suspend fun upsert(subscription: Subscription): SubscriptionId {
        pause()
        return writeLock.withLock {
            failNextUpsert?.let {
                failNextUpsert = null
                throw it
            }
            val id = if (subscription.id.value == 0L) SubscriptionId(nextId) else subscription.id
            nextId = maxOf(nextId, id.value + 1)
            state.value = state.value + (id to subscription.copy(id = id))
            upsertCount++
            id
        }
    }

    override suspend fun delete(id: SubscriptionId) {
        pause()
        writeLock.withLock {
            failNextDelete?.let {
                failNextDelete = null
                throw it
            }
            state.value = state.value - id
            deleteCount++
        }
    }

    override suspend fun replaceAll(subscriptions: List<Subscription>): Long {
        pause()
        return writeLock.withLock {
            replaceAllFailure.throwIfArmed()
            replaceAllGate?.let {
                replaceAllEntered.complete(Unit)
                // 가상 시간이 아니라 실제 시간으로 기다립니다(runTest의 시간 건너뛰기에 걸리지 않게).
                withContext(Dispatchers.Default) { withTimeout(GATE_TIMEOUT) { it.await() } }
            }
            val renumbered = subscriptions.associate { s ->
                val id = SubscriptionId(nextId++)
                id to s.copy(id = id)
            }
            state.value = renumbered
            replaceAllCount++
            ++rev
        }
    }

    override suspend fun revision(): Long {
        pause()
        failNextRevision?.let {
            failNextRevision = null
            throw it
        }
        return rev
    }

    override suspend fun bumpRevision(): Long {
        pause()
        return writeLock.withLock { ++rev }
    }

    private suspend fun pause() {
        val d = callDelay
        if (d > Duration.ZERO) delay(d)
    }
}
