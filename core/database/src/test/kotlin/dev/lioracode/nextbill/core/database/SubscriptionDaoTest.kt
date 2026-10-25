package dev.lioracode.nextbill.core.database

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionDaoTest : DatabaseTest() {
    private val dao get() = db.subscriptionDao()

    @Test
    fun upsertWithPeriodsThenObserve() = runTest {
        val id = dao.upsertWithPeriods(sub(), listOf(period(1, 10), period(20, 30), period(40)))
        dao.observe(id).test {
            val item = awaitItem()!!
            assertThat(item.subscription.name).isEqualTo("Netflix")
            assertThat(item.periods).hasSize(3)
            cancelAndIgnoreRemainingEvents()
        }
        dao.observeAll().test {
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertReplacesPeriods() = runTest {
        val id = dao.upsertWithPeriods(sub(), listOf(period(1, 10), period(20, 30), period(40)))
        dao.upsertWithPeriods(sub("Renamed", id), listOf(period(5)))
        val item = dao.get(id)!!
        assertThat(item.subscription.name).isEqualTo("Renamed")
        assertThat(item.periods.map { it.startEpochDay }).containsExactly(5L)
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun upsertReturnsRealIdOnInsertAndUpdate() = runTest {
        val first = dao.upsertWithPeriods(sub(), emptyList())
        val second = dao.upsertWithPeriods(sub("B"), emptyList())
        assertThat(first).isGreaterThan(0)
        assertThat(second).isNotEqualTo(first)
        assertThat(dao.upsertWithPeriods(sub("A2", first), emptyList())).isEqualTo(first)
        assertThat(dao.upsertWithPeriods(sub("B2", second), emptyList())).isEqualTo(second)
        assertThat(dao.count()).isEqualTo(2)
    }

    @Test
    fun deleteCascadesPeriods() = runTest {
        val id = dao.upsertWithPeriods(sub(), listOf(period(1), period(5)))
        dao.delete(id)
        assertThat(dao.get(id)).isNull()
        val rows = db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM active_periods")
        rows.use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(0)
        }
    }

    @Test
    fun replaceAllIsAtomicAndRenumbers() = runTest {
        val a = dao.upsertWithPeriods(sub("A"), listOf(period(1)))
        val b = dao.upsertWithPeriods(sub("B"), listOf(period(1)))
        dao.replaceAll(listOf(sub("C", 99) to listOf(period(7))))
        val all = dao.getAll()
        assertThat(all).hasSize(1)
        assertThat(all.single().subscription.name).isEqualTo("C")
        assertThat(all.single().subscription.id).isNotIn(listOf(a, b, 99L))
        assertThat(all.single().periods.single().startEpochDay).isEqualTo(7)
    }

    @Test
    fun replaceAllBumpsRevisionAtomically() = runTest {
        assertThat(dao.revision()).isNull()
        val rev = dao.replaceAll(listOf(sub("A") to listOf(period(1))))
        assertThat(rev).isEqualTo(1)
        assertThat(dao.revision()).isEqualTo(1)

        // Duplicate explicit rowId forces ABORT after deleteAll.
        val dupPeriods = listOf(
            ActivePeriodEntity(
                rowId = 5,
                subscriptionId = 0,
                startEpochDay = 1,
                endEpochDay = null
            ),
            ActivePeriodEntity(
                rowId = 5,
                subscriptionId = 0,
                startEpochDay = 2,
                endEpochDay = null
            )
        )
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.replaceAll(listOf(sub("Z") to dupPeriods)) }
        }
        assertThat(dao.revision()).isEqualTo(1)
        assertThat(dao.getAll().map { it.subscription.name }).containsExactly("A")
    }

    @Test
    fun bumpRevisionIncrements() = runTest {
        assertThat(dao.bumpRevision()).isEqualTo(1)
        assertThat(dao.bumpRevision()).isEqualTo(2)
        assertThat(dao.revision()).isEqualTo(2)
    }
}
