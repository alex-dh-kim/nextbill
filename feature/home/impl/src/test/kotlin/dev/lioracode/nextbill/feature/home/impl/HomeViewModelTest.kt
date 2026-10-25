package dev.lioracode.nextbill.feature.home.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.FakeCurrentDateSource
import dev.lioracode.nextbill.core.testing.FakeSettingsRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.MainDispatcherRule
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import dev.lioracode.nextbill.core.testing.usd
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule val mainRule = MainDispatcherRule(dispatcher)

    private fun vm(
        repo: FakeSubscriptionRepository,
        today: String,
        settings: UserSettings = UserSettings.DEFAULT,
        dates: FakeCurrentDateSource = FakeCurrentDateSource(d(today))
    ) = HomeViewModel(repo, FakeSettingsRepository(settings), dates, dispatcher)

    private suspend fun HomeViewModel.ready(): HomeUiState.Ready {
        var state: HomeUiState = HomeUiState.Loading
        uiState.test {
            state = awaitItem()
            while (state is HomeUiState.Loading) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return state as HomeUiState.Ready
    }

    @Test
    fun startsLoading() = runTest {
        val model = vm(FakeSubscriptionRepository(), "2026-11-15")
        assertThat(model.uiState.value).isEqualTo(HomeUiState.Loading)
    }

    @Test
    fun noSubscriptionsShowsZeroInDefaultCurrency() = runTest {
        val state = vm(FakeSubscriptionRepository(), "2026-11-15").ready()

        assertThat(state.due).isEqualTo(AmountSummary(krw(0), emptyList()))
        assertThat(state.upcoming).isEmpty()
        assertThat(state.month).isEqualTo(YearMonth.of(2026, 11))
    }

    @Test
    fun allCancelledButPaidThisMonthKeepsTotal() = runTest {
        val cancelled = sub(
            1,
            "넷플릭스",
            "2026-01-05",
            price = krw(17_000),
            periods = listOf(
                ActivePeriod(d("2026-01-05"), d("2026-11-10"))
            )
        )
        val state = vm(FakeSubscriptionRepository(listOf(cancelled)), "2026-11-15").ready()

        assertThat(state.upcoming).isEmpty()
        assertThat(state.due.primary).isEqualTo(krw(17_000))
    }

    @Test
    fun onlyFutureSubscriptionsDoesNotCrash() = runTest {
        val future = sub(1, "미래", "2026-12-01", price = krw(9_000))
        val state = vm(FakeSubscriptionRepository(listOf(future)), "2026-11-15").ready()

        assertThat(state.due.primary).isEqualTo(krw(0))
        assertThat(state.upcoming).hasSize(1)
        assertThat(state.upcoming.single().nextDate).isEqualTo(d("2026-12-01"))
    }

    @Test
    fun onlyUsdWithKrwDefault() = runTest {
        val usdSub = sub(1, "iCloud+", "2026-01-24", price = usd(299))
        val state = vm(FakeSubscriptionRepository(listOf(usdSub)), "2026-11-15").ready()

        assertThat(state.due.primary).isEqualTo(usd(299))
        assertThat(state.due.others).isEmpty()
    }

    @Test
    fun contentSortsByNextDateAndLabels() = runTest {
        val state = vm(FakeSubscriptionRepository(TestSubscriptions.all), "2026-11-15").ready()

        assertThat(state.upcoming.map { it.name }).containsExactly(
            "넷플릭스",
            "통신비",
            "유튜브 프리미엄",
            "iCloud+",
            "헬스장",
            "자동차보험"
        ).inOrder()
        assertThat(state.upcoming.map { it.label }).containsExactly(
            DueLabel.Today,
            DueLabel.Tomorrow,
            DueLabel.InDays(5),
            DueLabel.InDays(9),
            DueLabel.InDays(12),
            DueLabel.InDays(107)
        ).inOrder()
        assertThat(state.upcoming.first().cycle).isEqualTo(BillingCycle.Months(1))
        assertThat(
            state.fixedCost.primary
        ).isEqualTo(krw(17_000 + 55_000 + 14_900 + 60_000 + 40_000))
        assertThat(state.fixedCost.others).containsExactly(usd(299))
    }

    @Test
    fun dateChangeRecomputesWithoutDataChange() = runTest {
        val dates = FakeCurrentDateSource(d("2026-11-15"))
        val model = vm(FakeSubscriptionRepository(TestSubscriptions.all), "", dates = dates)

        model.uiState.test {
            var first = awaitItem()
            while (first is HomeUiState.Loading) first = awaitItem()
            first as HomeUiState.Ready
            assertThat(first.upcoming.first { it.name == "넷플릭스" }.label)
                .isEqualTo(DueLabel.Today)

            dates.set(d("2026-11-16"))
            val second = awaitItem() as HomeUiState.Ready
            // 넷플릭스는 12-15(29일 뒤)로 밀려 3-2 결제인 자동차보험 바로 앞에 놓입니다.
            assertThat(second.upcoming.map { it.name }).containsExactly(
                "통신비",
                "유튜브 프리미엄",
                "iCloud+",
                "헬스장",
                "넷플릭스",
                "자동차보험"
            ).inOrder()
            assertThat(second.upcoming[4].label).isEqualTo(DueLabel.InDays(29))
            assertThat(second.upcoming.first { it.name == "통신비" }.label)
                .isEqualTo(DueLabel.Today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthRolloverResetsTotals() = runTest {
        val dates = FakeCurrentDateSource(d("2026-11-30"))
        val novemberOnly = sub(
            7,
            "연간 구독",
            "2025-11-30",
            cycle = BillingCycle.Years(1),
            price = krw(100_000)
        )
        val repo = FakeSubscriptionRepository(TestSubscriptions.all + novemberOnly)
        val model = vm(repo, "", dates = dates)

        model.uiState.test {
            var first = awaitItem()
            while (first is HomeUiState.Loading) first = awaitItem()
            first as HomeUiState.Ready
            assertThat(first.month).isEqualTo(YearMonth.of(2026, 11))
            assertThat(first.due.primary).isEqualTo(krw(146_900 + 100_000))

            dates.set(d("2026-12-01"))
            val second = awaitItem() as HomeUiState.Ready
            assertThat(second.month).isEqualTo(YearMonth.of(2026, 12))
            // 12월: 넷플릭스·통신비·유튜브·iCloud·헬스장 (자동차보험은 3월)
            assertThat(second.due.primary).isEqualTo(krw(17_000 + 55_000 + 14_900 + 60_000))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updatesWhenRepositoryChanges() = runTest {
        val repo = FakeSubscriptionRepository()
        val model = vm(repo, "2026-11-15")

        model.uiState.test {
            var first = awaitItem()
            while (first is HomeUiState.Loading) first = awaitItem()
            assertThat((first as HomeUiState.Ready).upcoming).isEmpty()

            repo.upsert(TestSubscriptions.netflix)
            val second = awaitItem() as HomeUiState.Ready
            assertThat(second.upcoming.map { it.name }).containsExactly("넷플릭스")
            assertThat(second.due.primary).isEqualTo(krw(17_000))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
