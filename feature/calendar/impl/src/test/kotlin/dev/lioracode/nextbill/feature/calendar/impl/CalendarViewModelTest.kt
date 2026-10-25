package dev.lioracode.nextbill.feature.calendar.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.dueTotals
import dev.lioracode.nextbill.core.domain.billing.paymentsIn
import dev.lioracode.nextbill.core.domain.billing.summarize
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.FakeCurrentDateSource
import dev.lioracode.nextbill.core.testing.FakeSettingsRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.MainDispatcherRule
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import java.time.DayOfWeek
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule val mainRule = MainDispatcherRule(dispatcher)

    private val dates = FakeCurrentDateSource(d("2026-11-15"))

    private fun vm(
        subs: List<Subscription> = TestSubscriptions.all,
        handle: SavedStateHandle = SavedStateHandle()
    ) = CalendarViewModel(
        FakeSubscriptionRepository(subs),
        FakeSettingsRepository(UserSettings.DEFAULT),
        dates,
        dispatcher,
        handle
    ) { DayOfWeek.SUNDAY }

    private suspend fun CalendarViewModel.current(): CalendarUiState {
        var s: CalendarUiState? = null
        uiState.test {
            s = awaitItem()
            while (s == null) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return s!!
    }

    @Test fun startsOnTodayMonthAndSelection() = runTest {
        val s = vm().current()
        assertThat(s.month).isEqualTo(YearMonth.of(2026, 11))
        assertThat(s.selected).isEqualTo(d("2026-11-15"))
    }

    @Test fun dotsCappedAtThree() = runTest {
        val four = (1L..4L).map {
            sub(it, "S$it", "2026-01-20", colorKey = ColorKey.entries[it.toInt()])
        }
        val s = vm(four).current()
        assertThat(s.dots[d("2026-11-20")]).hasSize(3)
        assertThat(s.dots[d("2026-11-20")])
            .containsExactly(ColorKey.BLUE, ColorKey.GREEN, ColorKey.AMBER).inOrder()
        assertThat(s.paymentCounts[d("2026-11-20")]).isEqualTo(4)
    }

    @Test fun selectedListOnlyThatDay() = runTest {
        val model = vm()
        model.select(d("2026-11-15"))
        assertThat(model.current().selectedPayments.map { it.name }).containsExactly("넷플릭스")
        model.select(d("2026-11-16"))
        assertThat(model.current().selectedPayments.map { it.name }).containsExactly("통신비")
        model.select(d("2026-11-17"))
        assertThat(model.current().selectedPayments).isEmpty()
    }

    @Test fun countAndTotalMatchHome() = runTest {
        val s = vm().current()
        val month = YearMonth.of(2026, 11)
        val range = month.atDay(1)..month.atEndOfMonth()
        assertThat(s.monthCount).isEqualTo(paymentsIn(TestSubscriptions.all, range).size)
        assertThat(s.monthTotal)
            .isEqualTo(summarize(dueTotals(TestSubscriptions.all, month), CurrencyCode.KRW))
        assertThat(s.monthCount).isEqualTo(5)
    }

    @Test fun navigateMonthsAndBackToToday() = runTest {
        val model = vm()
        model.nextMonth()
        model.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 12))
            assertThat(it.selected).isEqualTo(d("2026-12-01"))
        }
        model.previousMonth()
        assertThat(model.current().selected).isEqualTo(d("2026-11-15"))
        model.nextMonth()
        model.goToToday()
        model.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 11))
            assertThat(it.selected).isEqualTo(d("2026-11-15"))
        }
    }

    @Test fun pausedMonthsShowNoDots() = runTest {
        val cancelled = sub(
            1,
            "넷플릭스",
            "2026-01-15",
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-05-20")))
        )
        val model = vm(listOf(cancelled))
        assertThat(model.current().dots).isEmpty()
        assertThat(model.current().monthCount).isEqualTo(0)
    }

    @Test fun monthBetweenTwoActivePeriodsShowsNoDots() = runTest {
        val resumed = sub(
            1,
            "넷플릭스",
            "2026-01-15",
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-03-20")),
                ActivePeriod(d("2026-08-10"), null)
            )
        )
        val model = vm(listOf(resumed))
        repeat(5) { model.previousMonth() }
        model.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 6))
            assertThat(it.dots).isEmpty()
        }
        repeat(5) { model.nextMonth() }
        assertThat(model.current().dots.keys).containsExactly(d("2026-11-15"))
    }

    @Test fun midnightMovesTodayWhenViewingCurrentMonth() = runTest {
        dates.set(d("2026-11-30"))
        val model = vm()
        assertThat(model.current().selected).isEqualTo(d("2026-11-30"))
        dates.set(d("2026-12-01"))
        model.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 12))
            assertThat(it.selected).isEqualTo(d("2026-12-01"))
            assertThat(it.today).isEqualTo(d("2026-12-01"))
        }
    }

    @Test fun midnightKeepsUserSelectedOtherMonth() = runTest {
        val model = vm()
        repeat(10) { model.previousMonth() }
        assertThat(model.current().month).isEqualTo(YearMonth.of(2026, 1))
        dates.set(d("2026-11-16"))
        model.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 1))
            assertThat(it.selected).isEqualTo(d("2026-01-01"))
            assertThat(it.today).isEqualTo(d("2026-11-16"))
        }
    }

    @Test fun restoresMonthAndSelectionFromSavedState() = runTest {
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        first.nextMonth()
        first.select(d("2026-12-25"))
        val second = vm(handle = handle)
        second.current().let {
            assertThat(it.month).isEqualTo(YearMonth.of(2026, 12))
            assertThat(it.selected).isEqualTo(d("2026-12-25"))
        }
    }
}
