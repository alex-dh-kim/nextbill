package dev.lioracode.nextbill.core.domain.validation

import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate

const val NAME_MAX_LENGTH = 30
const val MEMO_MAX_LENGTH = 500
const val MAX_MINOR = 1_000_000_000_000L

/** 편집 화면의 입력 상태. 금액은 사용자가 친 문자열 그대로 담습니다. */
data class SubscriptionDraft(
    val id: SubscriptionId = SubscriptionId(0),
    val name: String = "",
    val amountText: String = "",
    val currency: CurrencyCode,
    val interval: Int = 1,
    val unit: CycleUnit = CycleUnit.MONTHS,
    val anchorDate: LocalDate,
    val category: Category = Category.OTHER,
    val colorKey: ColorKey = ColorKey.BLUE,
    val memo: String = "",
    val reminder: Reminder
)

enum class CycleUnit { DAYS, WEEKS, MONTHS, YEARS }

/** [interval]은 1..365여야 합니다(아니면 `IllegalArgumentException`). */
fun CycleUnit.toCycle(interval: Int): BillingCycle = when (this) {
    CycleUnit.DAYS -> BillingCycle.Days(interval)
    CycleUnit.WEEKS -> BillingCycle.Weeks(interval)
    CycleUnit.MONTHS -> BillingCycle.Months(interval)
    CycleUnit.YEARS -> BillingCycle.Years(interval)
}

val BillingCycle.unit: CycleUnit
    get() = when (this) {
        is BillingCycle.Days -> CycleUnit.DAYS
        is BillingCycle.Weeks -> CycleUnit.WEEKS
        is BillingCycle.Months -> CycleUnit.MONTHS
        is BillingCycle.Years -> CycleUnit.YEARS
    }

enum class FieldError {
    NAME_BLANK,
    NAME_TOO_LONG,
    AMOUNT_BLANK,
    AMOUNT_INVALID,
    AMOUNT_TOO_LARGE,
    INTERVAL_INVALID,
    MEMO_TOO_LONG,
    ANCHOR_OUT_OF_RANGE
}
