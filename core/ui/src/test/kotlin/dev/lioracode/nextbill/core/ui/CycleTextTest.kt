package dev.lioracode.nextbill.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.BillingCycle
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CycleTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val d15 = LocalDate.of(2026, 1, 15)
    private val mar2 = LocalDate.of(2026, 3, 2) // 월요일

    private class Case(val expected: String, val actual: @Composable () -> String)

    private fun case(expected: String, actual: @Composable () -> String) = Case(expected, actual)

    // setContent는 테스트당 한 번만 부를 수 있어 한 컴포지션에서 모두 계산합니다.
    private fun verify(vararg cases: Case) {
        var out = emptyList<String>()
        rule.setContent { out = cases.map { it.actual() } }
        rule.waitForIdle()
        assertThat(out).containsExactlyElementsIn(cases.map { it.expected }).inOrder()
    }

    @Test
    @Config(qualifiers = "ko")
    fun koTable() = verify(
        case("매월 15일") { cycleText(BillingCycle.Months(1), d15) },
        case("3개월마다 15일") { cycleText(BillingCycle.Months(3), d15) },
        case("매년 3월 2일") { cycleText(BillingCycle.Years(1), mar2) },
        case("2년마다 3월 2일") { cycleText(BillingCycle.Years(2), mar2) },
        case("매주 월요일") { cycleText(BillingCycle.Weeks(1), mar2) },
        case("2주마다") { cycleText(BillingCycle.Weeks(2), mar2) },
        case("매일") { cycleText(BillingCycle.Days(1), mar2) },
        case("10일마다") { cycleText(BillingCycle.Days(10), mar2) }
    )

    @Test
    @Config(qualifiers = "ko")
    fun koHint() = verify(
        case("매월 15일에 결제돼요") { cycleChargeHint(BillingCycle.Months(1), d15) },
        case("3개월마다 15일에 결제돼요") { cycleChargeHint(BillingCycle.Months(3), d15) },
        case("매년 3월 2일에 결제돼요") { cycleChargeHint(BillingCycle.Years(1), mar2) },
        case("2년마다 3월 2일에 결제돼요") { cycleChargeHint(BillingCycle.Years(2), mar2) },
        case("매주 월요일에 결제돼요") { cycleChargeHint(BillingCycle.Weeks(1), mar2) },
        case("2주마다 결제돼요") { cycleChargeHint(BillingCycle.Weeks(2), mar2) },
        case("매일 결제돼요") { cycleChargeHint(BillingCycle.Days(1), mar2) },
        case("10일마다 결제돼요") { cycleChargeHint(BillingCycle.Days(10), mar2) }
    )

    @Test
    @Config(qualifiers = "en")
    fun en() = verify(
        case("Every month on day 15") { cycleText(BillingCycle.Months(1), d15) },
        case("Every 3 months on day 15") { cycleText(BillingCycle.Months(3), d15) },
        case("Every year on Mar 2") { cycleText(BillingCycle.Years(1), mar2) },
        case("Every 2 years on Mar 2") { cycleText(BillingCycle.Years(2), mar2) },
        case("Every Monday") { cycleText(BillingCycle.Weeks(1), mar2) },
        case("Every 2 weeks") { cycleText(BillingCycle.Weeks(2), mar2) },
        case("Every day") { cycleText(BillingCycle.Days(1), mar2) },
        case("Every 4 days") { cycleText(BillingCycle.Days(4), mar2) },
        case("Charged every month on day 15") { cycleChargeHint(BillingCycle.Months(1), d15) }
    )

    @Test
    @Config(qualifiers = "en")
    fun enHint() = verify(
        case("Charged every 3 months on day 15") { cycleChargeHint(BillingCycle.Months(3), d15) },
        case("Charged every year on Mar 2") { cycleChargeHint(BillingCycle.Years(1), mar2) },
        case("Charged every 2 years on Mar 2") { cycleChargeHint(BillingCycle.Years(2), mar2) },
        case("Charged every Monday") { cycleChargeHint(BillingCycle.Weeks(1), mar2) },
        case("Charged every 2 weeks") { cycleChargeHint(BillingCycle.Weeks(2), mar2) },
        case("Charged every day") { cycleChargeHint(BillingCycle.Days(1), mar2) },
        case("Charged every 4 days") { cycleChargeHint(BillingCycle.Days(4), mar2) }
    )
}
