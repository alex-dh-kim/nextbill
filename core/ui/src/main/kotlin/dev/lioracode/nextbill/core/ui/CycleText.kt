package dev.lioracode.nextbill.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.lioracode.nextbill.core.model.BillingCycle
import java.time.LocalDate

/** "매월 15일" / "Every month on day 15". [anchor]에서 일·월·요일을 읽습니다. */
@Composable
fun cycleText(cycle: BillingCycle, anchor: LocalDate): String = cycleString(
    cycle,
    anchor,
    CycleRes(
        R.string.cycle_months_1_day,
        R.string.cycle_months_n_day,
        R.string.cycle_years_1,
        R.string.cycle_years_n,
        R.string.cycle_weeks_1,
        R.string.cycle_weeks_n,
        R.string.cycle_days_1,
        R.string.cycle_days_n
    )
)

/** "매월 15일에 결제돼요" / "Charged every month on day 15". */
@Composable
fun cycleChargeHint(cycle: BillingCycle, anchor: LocalDate): String = cycleString(
    cycle,
    anchor,
    CycleRes(
        R.string.cycle_hint_months_1_day,
        R.string.cycle_hint_months_n_day,
        R.string.cycle_hint_years_1,
        R.string.cycle_hint_years_n,
        R.string.cycle_hint_weeks_1,
        R.string.cycle_hint_weeks_n,
        R.string.cycle_hint_days_1,
        R.string.cycle_hint_days_n
    )
)

private class CycleRes(
    @StringRes val months1: Int,
    @StringRes val monthsN: Int,
    @StringRes val years1: Int,
    @StringRes val yearsN: Int,
    @StringRes val weeks1: Int,
    @StringRes val weeksN: Int,
    @StringRes val days1: Int,
    @StringRes val daysN: Int
)

@Composable
private fun cycleString(cycle: BillingCycle, anchor: LocalDate, res: CycleRes): String {
    val n = cycle.interval
    val single = n == 1
    return when (cycle) {
        is BillingCycle.Months -> when {
            single -> stringResource(res.months1, anchor.dayOfMonth)
            else -> stringResource(res.monthsN, n, anchor.dayOfMonth)
        }

        is BillingCycle.Years -> when {
            single -> stringResource(res.years1, monthDayText(anchor))
            else -> stringResource(res.yearsN, n, monthDayText(anchor))
        }

        is BillingCycle.Weeks -> when {
            single -> stringResource(res.weeks1, weekdayFullText(anchor))
            else -> stringResource(res.weeksN, n)
        }

        is BillingCycle.Days -> when {
            single -> stringResource(res.days1)
            else -> stringResource(res.daysN, n)
        }
    }
}
