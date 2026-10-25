package dev.lioracode.nextbill.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.DefaultDispatcher
import dev.lioracode.nextbill.core.domain.billing.dueTotals
import dev.lioracode.nextbill.core.domain.billing.monthlyFixedCost
import dev.lioracode.nextbill.core.domain.billing.nextBillingDate
import dev.lioracode.nextbill.core.domain.billing.summarize
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: SubscriptionRepository,
    settings: SettingsRepository,
    dateSource: CurrentDateSource,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : ViewModel() {
    /** 합계는 해지 구독의 이번 달 결제까지 포함하고, 목록은 사용 중인 구독만 보여 줍니다. */
    val uiState: StateFlow<HomeUiState> =
        combine(repo.observeAll(), settings.settings, dateSource.today) { subs, s, today ->
            val month = YearMonth.from(today)
            HomeUiState.Ready(
                month = month,
                due = summarize(dueTotals(subs, month), s.defaultCurrency),
                fixedCost = summarize(monthlyFixedCost(subs), s.defaultCurrency),
                upcoming = upcomingItems(subs, today)
            )
        }
            .flowOn(dispatcher)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                HomeUiState.Loading
            )
}

private fun upcomingItems(subs: List<Subscription>, today: LocalDate): List<UpcomingItem> = subs
    .filter { it.status == SubscriptionStatus.ACTIVE }
    .mapNotNull { sub ->
        sub.nextBillingDate(today)?.let { next ->
            UpcomingItem(
                id = sub.id,
                name = sub.name,
                colorKey = sub.colorKey,
                cycle = sub.cycle,
                anchor = sub.anchorDate,
                price = sub.price,
                nextDate = next,
                label = dueLabel(next, today)
            )
        }
    }
    .sortedWith(compareBy<UpcomingItem> { it.nextDate }.thenBy { it.name })

private const val STOP_TIMEOUT_MS = 5_000L
