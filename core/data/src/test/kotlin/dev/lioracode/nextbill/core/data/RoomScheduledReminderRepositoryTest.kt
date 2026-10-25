package dev.lioracode.nextbill.core.data

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.database.NextBillDatabase
import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomScheduledReminderRepositoryTest {
    private lateinit var db: NextBillDatabase
    private lateinit var repo: RoomScheduledReminderRepository

    private fun reminder(id: Long, billing: String, trigger: String) =
        PlannedReminder(SubscriptionId(id), d(billing), at(trigger))

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NextBillDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RoomScheduledReminderRepository(db.scheduledReminderDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun getAllPutRemove() = runTest {
        val a = reminder(1, "2026-11-15", "2026-11-14T09:00+09:00")
        val b = reminder(2, "2026-11-20", "2026-11-19T09:00+09:00")
        repo.put(a)
        repo.put(b)
        assertThat(repo.getAll()).containsExactly(SubscriptionId(1), a, SubscriptionId(2), b)
        repo.remove(SubscriptionId(1))
        assertThat(repo.getAll()).containsExactly(SubscriptionId(2), b)
    }

    @Test
    fun removeIfMatchesBatchKeepsChangedRowAndReturnsCount() = runTest {
        val a = reminder(1, "2026-11-15", "2026-11-14T09:00+09:00")
        val b = reminder(2, "2026-11-20", "2026-11-19T09:00+09:00")
        val c = reminder(3, "2026-11-25", "2026-11-24T09:00+09:00")
        listOf(a, b, c).forEach { repo.put(it) }
        val newerB = reminder(2, "2026-12-20", "2026-12-19T09:00+09:00")
        repo.put(newerB)
        val removed = repo.removeIfMatches(listOf(a, b, c))
        assertThat(removed).isEqualTo(2)
        assertThat(repo.getAll()).containsExactly(SubscriptionId(2), newerB)
    }
}
