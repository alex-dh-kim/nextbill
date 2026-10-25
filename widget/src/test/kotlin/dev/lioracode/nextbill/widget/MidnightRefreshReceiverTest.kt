package dev.lioracode.nextbill.widget

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MidnightRefreshReceiverTest {
    private val events = mutableListOf<String>()

    @Test
    fun updatesThenReschedulesThenFinishes() = runTest {
        handleMidnight(this, { events += "update" }, { events += "next" }, { events += "finish" })
        runCurrent()
        assertThat(events).containsExactly("next", "update", "finish").inOrder()
    }

    @Test
    fun updateFailureStillFinishesAfterReschedule() = runTest {
        handleMidnight(
            this,
            { throw IOException("x") },
            { events += "next" },
            { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("next", "finish").inOrder()
    }

    @Test
    fun schedulesNextMidnightEvenWhenUpdateHangs() = runTest {
        handleMidnight(
            this,
            { awaitCancellation() },
            { events += "next" },
            { events += "finish" },
            budgetMs = 1_000
        )
        advanceTimeBy(1_001)
        runCurrent()
        assertThat(events).containsExactly("next", "finish").inOrder()
    }
}
