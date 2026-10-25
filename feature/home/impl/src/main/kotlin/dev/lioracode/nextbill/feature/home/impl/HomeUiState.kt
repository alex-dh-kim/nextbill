package dev.lioracode.nextbill.feature.home.impl

import dev.lioracode.nextbill.core.domain.billing.AmountSummary
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate
import java.time.YearMonth

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [upcoming]이 비면 목록 빈 상태를 보여 줍니다. 요약 카드는 그대로 실제 합계입니다. */
    data class Ready(
        val month: YearMonth,
        val due: AmountSummary,
        val fixedCost: AmountSummary,
        val upcoming: List<UpcomingItem>
    ) : HomeUiState
}

data class UpcomingItem(
    val id: SubscriptionId,
    val name: String,
    val colorKey: ColorKey,
    val cycle: BillingCycle,
    val anchor: LocalDate,
    val price: Money,
    val nextDate: LocalDate,
    val label: DueLabel
)
