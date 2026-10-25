package dev.lioracode.nextbill.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubscriptionDao {
    @Transaction
    @Query("SELECT * FROM subscriptions")
    abstract fun observeAll(): Flow<List<SubscriptionWithPeriods>>

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    abstract fun observe(id: Long): Flow<SubscriptionWithPeriods?>

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    abstract suspend fun get(id: Long): SubscriptionWithPeriods?

    @Transaction
    @Query("SELECT * FROM subscriptions")
    abstract suspend fun getAll(): List<SubscriptionWithPeriods>

    @Query("SELECT COUNT(*) FROM subscriptions")
    abstract suspend fun count(): Int

    @Query("DELETE FROM subscriptions WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT revision FROM dataset_meta WHERE id = 0")
    abstract suspend fun revision(): Long?

    @Upsert
    protected abstract suspend fun upsert(entity: SubscriptionEntity): Long

    @Insert
    protected abstract suspend fun insertPeriods(periods: List<ActivePeriodEntity>)

    @Query("DELETE FROM active_periods WHERE subscriptionId = :id")
    protected abstract suspend fun deletePeriods(id: Long)

    @Query("DELETE FROM subscriptions")
    protected abstract suspend fun deleteAll()

    @Upsert
    protected abstract suspend fun upsertMeta(meta: DatasetMetaEntity)

    /** 구독을 upsert하고 그 구독의 구간을 지운 뒤 다시 넣습니다. 실제 id를 돌려줍니다. */
    @Transaction
    open suspend fun upsertWithPeriods(
        entity: SubscriptionEntity,
        periods: List<ActivePeriodEntity>
    ): Long {
        val inserted = upsert(entity)
        val id = if (inserted == -1L) entity.id else inserted
        deletePeriods(id)
        insertPeriods(periods.map { it.copy(rowId = 0, subscriptionId = id) })
        return id
    }

    /** 전부 지우고 새 id로 넣은 뒤, 같은 트랜잭션에서 revision을 올려 돌려줍니다. */
    @Transaction
    open suspend fun replaceAll(
        items: List<Pair<SubscriptionEntity, List<ActivePeriodEntity>>>
    ): Long {
        deleteAll()
        items.forEach { (entity, periods) ->
            val id = upsert(entity.copy(id = 0))
            insertPeriods(periods.map { it.copy(subscriptionId = id) })
        }
        return bumpRevision()
    }

    @Transaction
    open suspend fun bumpRevision(): Long {
        val next = (revision() ?: 0L) + 1
        upsertMeta(DatasetMetaEntity(id = 0, revision = next))
        return next
    }
}
