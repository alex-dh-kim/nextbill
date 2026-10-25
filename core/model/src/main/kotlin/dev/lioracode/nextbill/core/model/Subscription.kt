package dev.lioracode.nextbill.core.model

import java.time.LocalDate

const val MAX_SUBSCRIPTIONS = 1_000
const val MAX_PERIODS = 50
val MIN_ANCHOR_DATE: LocalDate = LocalDate.of(1900, 1, 1)
val MAX_ANCHOR_DATE: LocalDate = LocalDate.of(2200, 12, 31)

enum class SubscriptionStatus { ACTIVE, CANCELLED }

/**
 * 구독. [periods]는 사용 구간 목록입니다(비어 있지 않음, 시간순, 겹치지 않음, 마지막만 열림,
 * 첫 구간 시작 == [anchorDate], 최대 [MAX_PERIODS]개).
 */
data class Subscription(
    val id: SubscriptionId,
    val name: String,
    val price: Money,
    val cycle: BillingCycle,
    val anchorDate: LocalDate,
    val category: Category,
    val colorKey: ColorKey,
    val memo: String,
    val reminder: Reminder,
    val periods: List<ActivePeriod>
) {
    init {
        require(anchorDate in MIN_ANCHOR_DATE..MAX_ANCHOR_DATE) { "anchorDate out of range" }
        require(periods.isNotEmpty()) { "periods must not be empty" }
        require(periods.size <= MAX_PERIODS) { "too many periods" }
        require(periods.first().start == anchorDate) { "first period must start at anchorDate" }
        periods.zipWithNext().forEach { (prev, next) ->
            val prevEnd = requireNotNull(prev.end) { "only the last period may be open" }
            require(next.start > prevEnd) { "periods must be ordered and must not overlap" }
        }
    }

    val status: SubscriptionStatus
        get() = if (periods.last().end ==
            null
        ) {
            SubscriptionStatus.ACTIVE
        } else {
            SubscriptionStatus.CANCELLED
        }

    val cancelledAt: LocalDate?
        get() = periods.last().end
}
