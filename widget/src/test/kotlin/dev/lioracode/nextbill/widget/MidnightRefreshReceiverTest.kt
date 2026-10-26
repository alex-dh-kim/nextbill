package dev.lioracode.nextbill.widget

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.DataCoordinator
import dev.lioracode.nextbill.core.domain.usecase.DerivedStateRefresher
import dev.lioracode.nextbill.core.domain.usecase.SyncRemindersUseCase
import dev.lioracode.nextbill.core.testing.FakeBackupCodec
import dev.lioracode.nextbill.core.testing.FakeCurrentDateSource
import dev.lioracode.nextbill.core.testing.FakeDayChangeScheduler
import dev.lioracode.nextbill.core.testing.FakeImportStaging
import dev.lioracode.nextbill.core.testing.FakeReminderAlarm
import dev.lioracode.nextbill.core.testing.FakeReminderPoster
import dev.lioracode.nextbill.core.testing.FakeScheduledReminderRepository
import dev.lioracode.nextbill.core.testing.FakeSettingsRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.FakeWidgetUpdater
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun recomputesDateThenReschedulesThenRefreshesThenFinishes() = runTest {
        handleMidnight(
            this,
            { events += "date" },
            { events += "refresh" },
            { events += "next" },
            { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("date", "next", "refresh", "finish").inOrder()
    }

    @Test
    fun refreshFailureStillFinishesAfterReschedule() = runTest {
        handleMidnight(
            this,
            { throw IllegalStateException("date") },
            { throw IOException("x") },
            { events += "next" },
            { events += "finish" }
        )
        runCurrent()
        assertThat(events).containsExactly("next", "finish").inOrder()
    }

    @Test
    fun schedulesNextMidnightEvenWhenRefreshHangs() = runTest {
        handleMidnight(
            this,
            {},
            { awaitCancellation() },
            { events += "next" },
            { events += "finish" },
            budgetMs = 1_000
        )
        advanceTimeBy(1_001)
        runCurrent()
        assertThat(events).containsExactly("next", "finish").inOrder()
    }

    /**
     * 실제 refresher로: 이미 떠 있던 프로세스의 날짜 원천이 아직 어제(자정 타이머가 늦음)여도, 자정 처리가
     * 날짜를 다시 계산해 어제 결제일의 알림을 정리하고 위젯을 갱신합니다.
     */
    @Test
    fun midnightRecomputesStaleDateThenPrunesAndUpdatesWidget() = runTest {
        val yesterday = d("2026-11-15")
        val today = d("2026-11-16")
        // backgroundScope 작업은 advanceUntilIdle이 기다리지 않으므로 같은 스케줄러의 별도 Job을 씁니다.
        val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val coordinator = DataCoordinator()
        val repo = FakeSubscriptionRepository(listOf(TestSubscriptions.netflix))
        val poster = FakeReminderPoster()
        val widget = FakeWidgetUpdater()
        val dayChange = FakeDayChangeScheduler()
        val dates = FakeCurrentDateSource(yesterday)
        val sync = SyncRemindersUseCase(
            repo,
            FakeScheduledReminderRepository(),
            FakeReminderAlarm(),
            MutableClock()
        )
        val refresher = DerivedStateRefresher(
            coordinator,
            repo,
            sync,
            poster,
            widget,
            dayChange,
            dates,
            FailureReporter.None,
            appScope
        )
        AppStartup(
            coordinator,
            repo,
            FakeSettingsRepository(),
            FakeBackupCodec(),
            FakeImportStaging(),
            sync,
            refresher,
            FailureReporter.None
        ).start()
        advanceUntilIdle()
        poster.show(listOf(ReminderPost.Single(TestSubscriptions.netflix, yesterday)), yesterday)
        // 진짜 시계는 이미 오늘이지만 날짜 원천은 아직 어제입니다.
        dates.dateOnRefresh = today

        // 대조: 날짜를 다시 계산하지 않으면 어제 알림이 남습니다.
        handleMidnight(this, {}, refresher::refreshNow, dayChange::scheduleNext, {})
        advanceUntilIdle()
        assertThat(poster.active).isNotEmpty()

        handleMidnight(
            this,
            dates::refresh,
            refresher::refreshNow,
            dayChange::scheduleNext,
            { events += "finish" }
        )
        advanceUntilIdle()

        assertThat(dates.today.value).isEqualTo(today)
        assertThat(poster.active).isEmpty()
        assertThat(widget.updateCount).isEqualTo(3) // 시작 1번 + 대조 1번 + 자정 1번
        assertThat(events).containsExactly("finish")
        appScope.cancel()
    }
}
