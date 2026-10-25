package dev.lioracode.nextbill.core.data.backup

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.data.backup.BackupJsonFixture.period
import dev.lioracode.nextbill.core.data.backup.BackupJsonFixture.periods
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.validation.MAX_MINOR
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.testing.MutableClock
import org.junit.Test

class BackupValidationTest {
    private val codec = JsonBackupCodec(MutableClock())

    private val cases: Map<String, String> = mapOf(
        "interval 0" to BackupJsonFixture.valid(interval = "0"),
        "interval 366" to BackupJsonFixture.valid(interval = "366"),
        "currency GBP" to BackupJsonFixture.valid(currency = "GBP"),
        "priceMinor 0" to BackupJsonFixture.valid(priceMinor = "0"),
        "priceMinor -1" to BackupJsonFixture.valid(priceMinor = "-1"),
        "priceMinor MAX+1" to BackupJsonFixture.valid(priceMinor = (MAX_MINOR + 1).toString()),
        "priceMinor fractional" to BackupJsonFixture.valid(priceMinor = "10.5"),
        "daysBefore 2" to BackupJsonFixture.valid(daysBefore = "2"),
        "time 25:00" to BackupJsonFixture.valid(time = "25:00"),
        "time 9:00" to BackupJsonFixture.valid(time = "9:00"),
        "time with seconds" to BackupJsonFixture.valid(time = "09:00:30"),
        "name empty" to BackupJsonFixture.valid(name = ""),
        "name blank" to BackupJsonFixture.valid(name = "   "),
        "name 31" to BackupJsonFixture.valid(name = "a".repeat(31)),
        "memo 501" to BackupJsonFixture.valid(memo = "a".repeat(501)),
        "anchor 1800" to BackupJsonFixture.valid(
            anchorDate = "1800-01-01",
            periods = periods(period("1800-01-01", null))
        ),
        "anchor 2201" to BackupJsonFixture.valid(
            anchorDate = "2201-01-01",
            periods = periods(period("2201-01-01", null))
        ),
        "period end out of range" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-15", "2300-01-01"))
        ),
        "date not a date" to BackupJsonFixture.valid(anchorDate = "2026-02-30"),
        "periods overlap" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-15", "2026-03-01"), period("2026-02-01", null))
        ),
        "periods touch (next.start == prev.end)" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-15", "2026-03-01"), period("2026-03-01", null))
        ),
        "period reversed" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-15", "2026-01-10"))
        ),
        "open period in the middle" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-15", null), period("2026-05-01", null))
        ),
        "first.start != anchor" to BackupJsonFixture.valid(
            periods = periods(period("2026-01-16", null))
        ),
        "periods empty" to BackupJsonFixture.valid(periods = "[]"),
        "category FOO" to BackupJsonFixture.valid(category = "FOO"),
        "unit FORTNIGHTS" to BackupJsonFixture.valid(unit = "FORTNIGHTS"),
        "1001 subscriptions" to BackupJsonFixture.valid(subscriptionCount = MAX_SUBSCRIPTIONS + 1),
        "51 periods" to BackupJsonFixture.valid(
            periods = (0..MAX_PERIODS).map { i ->
                val start = java.time.LocalDate.parse("2026-01-15").plusDays(i * 10L)
                val end = if (i == MAX_PERIODS) null else start.plusDays(3).toString()
                period(start.toString(), end)
            }.joinToString(",", "[", "]")
        ),
        "settings daysBefore 5" to BackupJsonFixture.valid(
            settingsReminder = """{"enabled":true,"daysBefore":5,"time":"09:00"}"""
        ),
        "settings time 24:00" to BackupJsonFixture.valid(
            settingsReminder = """{"enabled":true,"daysBefore":1,"time":"24:00"}"""
        ),
        "settings currency GBP" to BackupJsonFixture.valid(defaultCurrency = "GBP"),
        "missing field" to BackupJsonFixture.valid().replace("\"colorKey\":\"RED\",", "")
    )

    @Test
    fun eachInvalidValueIsMalformed() {
        val failures = cases.filter { (_, json) ->
            codec.decode(json) != Outcome.Err(BackupError.Malformed)
        }.keys
        assertThat(failures).isEmpty()
    }

    @Test
    fun exactLimitsAreAccepted() {
        assertThat(codec.decode(BackupJsonFixture.valid(subscriptionCount = MAX_SUBSCRIPTIONS)))
            .isInstanceOf(Outcome.Ok::class.java)
        assertThat(codec.decode(BackupJsonFixture.valid(priceMinor = MAX_MINOR.toString())))
            .isInstanceOf(Outcome.Ok::class.java)
        assertThat(
            codec.decode(
                BackupJsonFixture.valid(interval = "365", daysBefore = "7", time = "23:59")
            )
        )
            .isInstanceOf(Outcome.Ok::class.java)
        assertThat(codec.decode(BackupJsonFixture.valid(memo = "a".repeat(500))))
            .isInstanceOf(Outcome.Ok::class.java)
    }

    @Test
    fun unknownKeysAreIgnoredButUnknownEnumsAreNot() {
        val withExtra = BackupJsonFixture.valid().replace("\"memo\"", "\"extra\":1,\"memo\"")
        assertThat(codec.decode(withExtra)).isInstanceOf(Outcome.Ok::class.java)
        assertThat(codec.decode(BackupJsonFixture.valid().replace("SYSTEM", "SEPIA")))
            .isEqualTo(Outcome.Err(BackupError.Malformed))
    }
}
