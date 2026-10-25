package dev.lioracode.nextbill.core.domain.repository

import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeActive(): Flow<List<Subscription>>

    /** [Subscription.cancelledAt] 내림차순. */
    fun observeCancelled(): Flow<List<Subscription>>

    fun observeAll(): Flow<List<Subscription>>

    fun observe(id: SubscriptionId): Flow<Subscription?>

    suspend fun get(id: SubscriptionId): Subscription?

    suspend fun getAll(): List<Subscription>

    suspend fun count(): Int

    /** 구독과 사용 구간을 한 트랜잭션으로 저장합니다. id가 0이면 새로 매깁니다. */
    suspend fun upsert(subscription: Subscription): SubscriptionId

    suspend fun delete(id: SubscriptionId)

    /**
     * 한 트랜잭션으로 전부 바꿉니다. 입력 id는 무시하고 새로 매기며, 같은 트랜잭션에서
     * 데이터 세대([revision])를 1 올린 뒤 그 값을 돌려줍니다.
     */
    suspend fun replaceAll(subscriptions: List<Subscription>): Long

    /** 영속 데이터 세대. 처음에는 0입니다. */
    suspend fun revision(): Long

    /** "가져오기 취소"용. 세대를 1 올리고 그 값을 돌려줍니다. */
    suspend fun bumpRevision(): Long
}
