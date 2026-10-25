package dev.lioracode.nextbill.core.database

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduledReminderDaoTest : DatabaseTest() {
    private val dao get() = db.scheduledReminderDao()

    @Test
    fun scheduledReminderSurvivesSubscriptionDelete() = runTest {
        val id = db.subscriptionDao().upsertWithPeriods(sub(), listOf(period(1)))
        dao.put(ScheduledReminderEntity(id, 100, 5_000))
        db.subscriptionDao().delete(id)
        assertThat(dao.getAll()).containsExactly(ScheduledReminderEntity(id, 100, 5_000))
    }

    @Test
    fun putUpsertsAndRemoveDeletes() = runTest {
        dao.put(ScheduledReminderEntity(1, 100, 5_000))
        dao.put(ScheduledReminderEntity(1, 200, 6_000))
        assertThat(dao.getAll()).containsExactly(ScheduledReminderEntity(1, 200, 6_000))
        dao.remove(1)
        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun removeIfMatchesKeepsNewerRow() = runTest {
        dao.put(ScheduledReminderEntity(1, 200, 6_000))
        assertThat(dao.removeIfMatches(1, 100, 5_000)).isEqualTo(0)
        assertThat(dao.getAll()).hasSize(1)
        assertThat(dao.removeIfMatches(1, 200, 6_000)).isEqualTo(1)
        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun removeAllIfMatchesInOneTransaction() = runTest {
        val a = ScheduledReminderEntity(1, 100, 5_000)
        val b = ScheduledReminderEntity(2, 100, 5_000)
        val c = ScheduledReminderEntity(3, 100, 5_000)
        listOf(a, b, c).forEach { dao.put(it) }
        val changed = c.copy(billingEpochDay = 300)
        dao.put(changed)
        val removed = dao.removeAllIfMatches(listOf(a, b, c))
        assertThat(removed).isEqualTo(2)
        assertThat(dao.getAll()).containsExactly(changed)
    }
}
