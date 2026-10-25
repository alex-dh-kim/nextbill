package dev.lioracode.nextbill.feature.calendar.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.DefaultDispatcher
import dev.lioracode.nextbill.core.domain.billing.dueTotals
import dev.lioracode.nextbill.core.domain.billing.paymentsIn
import dev.lioracode.nextbill.core.domain.billing.summarize
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel(
    repo: SubscriptionRepository,
    settings: SettingsRepository,
    private val dateSource: CurrentDateSource,
    dispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
    firstDayOfWeek: () -> DayOfWeek
) : ViewModel() {
    @Inject
    constructor(
        repo: SubscriptionRepository,
        settings: SettingsRepository,
        dateSource: CurrentDateSource,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle
    ) : this(
        repo,
        settings,
        dateSource,
        dispatcher,
        savedStateHandle,
        { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    )

    /** 표시 월·선택일·오늘을 한 값으로 묶어, 자정에 중간 상태가 화면에 나가지 않게 합니다. */
    private data class View(val month: YearMonth, val selected: LocalDate, val today: LocalDate)

    private val view: MutableStateFlow<View>

    init {
        val today = dateSource.today.value
        val month = savedStateHandle.get<String>(KEY_MONTH)?.let {
            parseOrNull { YearMonth.parse(it) }
        }
        val selected = savedStateHandle.get<String>(KEY_SELECTED)?.let {
            parseOrNull { LocalDate.parse(it) }
        }
        val m = month ?: YearMonth.from(today)
        view = MutableStateFlow(
            View(
                m,
                selected?.takeIf {
                    YearMonth.from(it) == m
                } ?: defaultSelection(m, today),
                today
            )
        )
        viewModelScope.launch {
            dateSource.today.collect { newToday ->
                view.update { v ->
                    when {
                        newToday == v.today -> v

                        v.month == YearMonth.from(v.today) ->
                            View(YearMonth.from(newToday), newToday, newToday)

                        else -> v.copy(today = newToday)
                    }
                }
                persist()
            }
        }
    }

    val uiState: StateFlow<CalendarUiState?> =
        combine(view, repo.observeAll(), settings.settings) { v, subs, s ->
            val payments = paymentsIn(subs, v.month.atDay(1)..v.month.atEndOfMonth())
            val byDate = payments.groupBy { it.date }
            CalendarUiState(
                month = v.month,
                today = v.today,
                selected = v.selected,
                dots = byDate.mapValues { (_, list) ->
                    list.take(MAX_DOTS).map { it.subscription.colorKey }
                },
                paymentCounts = byDate.mapValues { it.value.size },
                monthCount = payments.size,
                monthTotal = summarize(dueTotals(subs, v.month), s.defaultCurrency),
                selectedPayments = byDate[v.selected].orEmpty().map {
                    CalendarPayment(
                        it.subscription.id,
                        it.subscription.name,
                        it.subscription.colorKey,
                        it.subscription.price,
                        it.subscription.cycle,
                        it.subscription.anchorDate
                    )
                },
                firstDayOfWeek = firstDayOfWeek()
            )
        }
            .flowOn(dispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun previousMonth() = moveMonth(-1)

    fun nextMonth() = moveMonth(1)

    fun goToToday() {
        view.update { it.copy(month = YearMonth.from(it.today), selected = it.today) }
        persist()
    }

    fun select(date: LocalDate) {
        view.update { it.copy(month = YearMonth.from(date), selected = date) }
        persist()
    }

    private fun moveMonth(delta: Long) {
        view.update {
            val m = it.month.plusMonths(delta)
            it.copy(month = m, selected = defaultSelection(m, it.today))
        }
        persist()
    }

    private fun persist() {
        val v = view.value
        savedStateHandle[KEY_MONTH] = v.month.toString()
        savedStateHandle[KEY_SELECTED] = v.selected.toString()
    }

    private companion object {
        const val KEY_MONTH = "month"
        const val KEY_SELECTED = "selected"
        const val MAX_DOTS = 3
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun defaultSelection(month: YearMonth, today: LocalDate): LocalDate =
    if (YearMonth.from(today) == month) today else month.atDay(1)

private inline fun <T> parseOrNull(block: () -> T): T? = try {
    block()
} catch (_: DateTimeParseException) {
    null
}
