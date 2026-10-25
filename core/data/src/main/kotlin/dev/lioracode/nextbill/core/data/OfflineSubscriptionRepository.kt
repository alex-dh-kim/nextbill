package dev.lioracode.nextbill.core.data

import dev.lioracode.nextbill.core.data.mapping.toEntities
import dev.lioracode.nextbill.core.data.mapping.toModel
import dev.lioracode.nextbill.core.database.SubscriptionDao
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineSubscriptionRepository @Inject constructor(private val dao: SubscriptionDao) :
    SubscriptionRepository {
    override fun observeActive(): Flow<List<Subscription>> = observeAll().map { list ->
        list.filter { it.status == SubscriptionStatus.ACTIVE }
    }

    override fun observeCancelled(): Flow<List<Subscription>> = observeAll().map { list ->
        list.filter { it.status == SubscriptionStatus.CANCELLED }
            .sortedWith(compareByDescending<Subscription> { it.cancelledAt }.thenBy { it.id.value })
    }

    override fun observeAll(): Flow<List<Subscription>> =
        dao.observeAll().map { list -> list.map { it.toModel() }.sortedBy { it.id.value } }

    override fun observe(id: SubscriptionId): Flow<Subscription?> =
        dao.observe(id.value).map { it?.toModel() }

    override suspend fun get(id: SubscriptionId): Subscription? = dao.get(id.value)?.toModel()

    override suspend fun getAll(): List<Subscription> = dao.getAll().map {
        it.toModel()
    }.sortedBy { it.id.value }

    override suspend fun count(): Int = dao.count()

    override suspend fun upsert(subscription: Subscription): SubscriptionId {
        val (entity, periods) = subscription.toEntities()
        return SubscriptionId(dao.upsertWithPeriods(entity, periods))
    }

    override suspend fun delete(id: SubscriptionId) = dao.delete(id.value)

    override suspend fun replaceAll(subscriptions: List<Subscription>): Long =
        dao.replaceAll(subscriptions.map { it.toEntities() })

    override suspend fun revision(): Long = dao.revision() ?: 0L

    override suspend fun bumpRevision(): Long = dao.bumpRevision()
}
