package dev.lioracode.nextbill.core.data.mapping

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.data.mapping.toEntities
import dev.lioracode.nextbill.core.database.SubscriptionWithPeriods
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import dev.lioracode.nextbill.core.testing.usd
import java.time.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionMappingTest {
    @Test
    fun mappingRoundTripsEveryField() {
        val original = sub(
            id = 7,
            name = "Spotify",
            anchor = "2026-01-15",
            cycle = BillingCycle.Weeks(2),
            price = usd(1_299),
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-02-01")),
                ActivePeriod(d("2026-03-01"), d("2026-04-01")),
                ActivePeriod(d("2026-05-01"), null)
            ),
            reminder = Reminder(true, 3, LocalTime.of(21, 30)),
            category = Category.MUSIC,
            colorKey = ColorKey.VIOLET
        ).copy(memo = "family plan")
        val (entity, periods) = original.toEntities()
        assertThat(periods).hasSize(3)
        // Room 관계 조회는 순서를 보장하지 않으므로 뒤섞어 넘깁니다.
        val back = SubscriptionWithPeriods(entity, periods.reversed()).toModel()
        assertThat(back).isEqualTo(original)
    }

    @Test
    fun unknownEnumStringFallsBack() {
        val (entity, periods) = sub().toEntities()
        val model = SubscriptionWithPeriods(
            entity.copy(category = "BOGUS", colorKey = "bogus", cycleUnit = "FORTNIGHTS"),
            periods
        ).toModel()
        assertThat(model.category).isEqualTo(Category.OTHER)
        assertThat(model.colorKey).isEqualTo(ColorKey.BLUE)
        assertThat(model.cycle).isEqualTo(BillingCycle.Months(1))
    }
}
