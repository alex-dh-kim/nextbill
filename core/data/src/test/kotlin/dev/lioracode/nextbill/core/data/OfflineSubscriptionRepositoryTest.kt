package dev.lioracode.nextbill.core.data

import android.content.Context
import androidx.room.Room
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.database.NextBillDatabase
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OfflineSubscriptionRepositoryTest {
    private lateinit var db: NextBillDatabase
    private lateinit var repo: OfflineSubscriptionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NextBillDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = OfflineSubscriptionRepository(db.subscriptionDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun repositorySplitsActiveAndCancelled() = runTest {
        val active = sub(id = 0, name = "Active")
        val old = sub(
            id = 0,
            name = "Old",
            anchor = "2026-01-01",
            periods = listOf(ActivePeriod(d("2026-01-01"), d("2026-02-01")))
        )
        val recent = sub(
            id = 0,
            name = "Recent",
            anchor = "2026-01-01",
            periods = listOf(ActivePeriod(d("2026-01-01"), d("2026-03-01")))
        )
        repo.upsert(active)
        repo.upsert(old)
        repo.upsert(recent)
        repo.observeActive().test {
            assertThat(awaitItem().map { it.name }).containsExactly("Active")
            cancelAndIgnoreRemainingEvents()
        }
        repo.observeCancelled().test {
            assertThat(awaitItem().map { it.name }).containsExactly("Recent", "Old").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.count()).isEqualTo(3)
        repo.observeAll().test {
            assertThat(awaitItem().map { it.name }).containsExactly("Active", "Old", "Recent")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.getAll()).hasSize(3)
    }

    @Test
    fun activeAndAllAreOrderedByIdRegardlessOfInsertOrder() = runTest {
        repo.upsert(sub(id = 30, name = "C"))
        repo.upsert(sub(id = 10, name = "A"))
        repo.upsert(sub(id = 20, name = "B"))
        repo.observeActive().test {
            assertThat(awaitItem().map { it.id.value }).containsExactly(10L, 20L, 30L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        repo.observeAll().test {
            assertThat(awaitItem().map { it.id.value }).containsExactly(10L, 20L, 30L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.getAll().map { it.id.value }).containsExactly(10L, 20L, 30L).inOrder()
    }

    @Test
    fun cancelledTieOnDateBrokenById() = runTest {
        fun cancelled(id: Long, end: String) = sub(
            id = id,
            anchor = "2026-01-01",
            periods = listOf(ActivePeriod(d("2026-01-01"), d(end)))
        )
        repo.upsert(cancelled(5, "2026-03-01"))
        repo.upsert(cancelled(3, "2026-03-01"))
        repo.upsert(cancelled(4, "2026-04-01"))
        repo.observeCancelled().test {
            assertThat(awaitItem().map { it.id.value }).containsExactly(4L, 3L, 5L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertGetObserveDelete() = runTest {
        val id = repo.upsert(sub(id = 0, name = "A"))
        assertThat(id.value).isGreaterThan(0)
        assertThat(repo.get(id)?.name).isEqualTo("A")
        repo.observe(id).test {
            assertThat(awaitItem()?.name).isEqualTo("A")
            repo.delete(id)
            assertThat(awaitItem()).isNull()
        }
        assertThat(repo.get(SubscriptionId(999))).isNull()
    }

    @Test
    fun replaceAllReturnsNewRevisionAndBumpDelegates() = runTest {
        assertThat(repo.revision()).isEqualTo(0)
        assertThat(repo.replaceAll(listOf(sub(id = 5), sub(id = 9)))).isEqualTo(1)
        assertThat(repo.count()).isEqualTo(2)
        assertThat(repo.bumpRevision()).isEqualTo(2)
        assertThat(repo.revision()).isEqualTo(2)
    }

    @Test
    fun revisionPersistsAcrossReopen() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val name = "reopen-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, NextBillDatabase::class.java, name)
            .allowMainThreadQueries().build()
        val first = open()
        val firstRepo = OfflineSubscriptionRepository(first.subscriptionDao())
        assertThat(firstRepo.revision()).isEqualTo(0)
        firstRepo.replaceAll(listOf(sub(id = 0)))
        first.close()
        val second = open()
        try {
            assertThat(OfflineSubscriptionRepository(second.subscriptionDao()).revision())
                .isEqualTo(1)
        } finally {
            second.close()
            context.deleteDatabase(name)
            assertThat(File(context.getDatabasePath(name).path).exists()).isFalse()
        }
    }
}
