package dev.lioracode.nextbill.core.data.backup

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.backup.MAX_BACKUP_CHARS
import dev.lioracode.nextbill.core.domain.validation.MEMO_MAX_LENGTH
import dev.lioracode.nextbill.core.domain.validation.NAME_MAX_LENGTH
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.MAX_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.MIN_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import java.time.Instant
import java.time.LocalTime
import org.junit.Test

class JsonBackupCodecTest {
    private val clock = MutableClock(Instant.parse("2026-11-10T01:00:00Z"))
    private val codec = JsonBackupCodec(clock)

    private fun withZeroIds(s: BackupSnapshot) =
        s.copy(subscriptions = s.subscriptions.map { it.copy(id = SubscriptionId(0)) })

    private fun ok(snapshot: BackupSnapshot) = Outcome.Ok(withZeroIds(snapshot))

    @Test
    fun roundTripIsLossless() {
        val cancelled = sub(
            7,
            "해지됨",
            "2026-01-15",
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-03-01")))
        )
        val threePeriods = sub(
            8,
            "구간 셋",
            "2025-01-01",
            cycle = BillingCycle.Weeks(2),
            price = Money(123, CurrencyCode.USD),
            reminder = Reminder(false, 7, LocalTime.of(21, 30)),
            periods = listOf(
                ActivePeriod(d("2025-01-01"), d("2025-02-01")),
                ActivePeriod(d("2025-03-01"), d("2025-04-01")),
                ActivePeriod(d("2025-06-01"), null)
            )
        )
        val snapshot = BackupSnapshot(
            TestSubscriptions.all + cancelled + threePeriods,
            UserSettings(
                Reminder(true, 3, LocalTime.of(8, 5)),
                ThemeMode.DARK,
                true,
                CurrencyCode.JPY
            )
        )

        assertThat(codec.decode(codec.encode(snapshot))).isEqualTo(ok(snapshot))
    }

    @Test
    fun encodeUsesClockForExportedAt() {
        val json = codec.encode(BackupSnapshot(emptyList(), UserSettings.DEFAULT))
        assertThat(json).contains("\"exportedAt\":\"2026-11-10T01:00:00Z\"")
        assertThat(json).contains("\"schemaVersion\":1")
    }

    @Test
    fun encodeNeverWritesIds() {
        val json = codec.encode(BackupSnapshot(TestSubscriptions.all, UserSettings.DEFAULT))
        assertThat(json).doesNotContain("\"id\"")
    }

    @Test
    fun decodesCommittedV1File() {
        val json = checkNotNull(javaClass.classLoader!!.getResource("backup-v1.json")).readText()

        val result = codec.decode(json) as Outcome.Ok<BackupSnapshot>
        val s = result.value.subscriptions.single()
        assertThat(s.id).isEqualTo(SubscriptionId(0))
        assertThat(s.name).isEqualTo("넷플릭스")
        assertThat(s.price).isEqualTo(krw(17_000))
        assertThat(s.cycle).isEqualTo(BillingCycle.Months(1))
        assertThat(s.anchorDate).isEqualTo(d("2026-01-15"))
        assertThat(s.category).isEqualTo(Category.VIDEO)
        assertThat(s.colorKey).isEqualTo(ColorKey.RED)
        assertThat(s.memo).isEmpty()
        assertThat(s.reminder).isEqualTo(Reminder(true, 1, LocalTime.of(9, 0)))
        assertThat(s.periods).containsExactly(
            ActivePeriod(d("2026-01-15"), d("2026-01-20")),
            ActivePeriod(d("2026-04-20"), null)
        ).inOrder()
        assertThat(result.value.settings).isEqualTo(
            UserSettings(
                Reminder(true, 1, LocalTime.of(9, 0)),
                ThemeMode.SYSTEM,
                false,
                CurrencyCode.KRW
            )
        )
    }

    @Test
    fun newerVersionIsRejected() {
        assertThat(
            codec.decode("""{"schemaVersion":2}""")
        ).isEqualTo(Outcome.Err(BackupError.UnsupportedVersion(2)))
    }

    @Test
    fun badVersions() {
        listOf(
            """{"schemaVersion":0}""",
            """{"schemaVersion":-1}""",
            """{"schemaVersion":"1"}""",
            """{"schemaVersion":1.5}""",
            "{}",
            "[]"
        )
            .forEach { assertThat(codec.decode(it)).isEqualTo(Outcome.Err(BackupError.Malformed)) }
    }

    @Test
    fun garbageIsMalformed() {
        listOf("not json", "{}", """{"schemaVersion":1}""", "", "null")
            .forEach { assertThat(codec.decode(it)).isEqualTo(Outcome.Err(BackupError.Malformed)) }
    }

    @Test
    fun tooLarge() {
        assertThat(
            codec.decode(" ".repeat(MAX_BACKUP_CHARS + 1))
        ).isEqualTo(Outcome.Err(BackupError.TooLarge))
    }

    @Test
    fun emptySubscriptionsIsValid() {
        val snapshot = BackupSnapshot(emptyList(), UserSettings.DEFAULT)
        assertThat(codec.decode(codec.encode(snapshot))).isEqualTo(Outcome.Ok(snapshot))
    }

    @Test
    fun largestValidStateRoundTrips() {
        // JSON 이스케이프가 가장 긴 문자: 제어 문자는 6자(\u0001), `"`·`\`·`\n`은 2자.
        val memo = buildString { repeat(MEMO_MAX_LENGTH) { append('\u0001') } }
        val name = "\u0001".repeat(NAME_MAX_LENGTH)
        val periods = (0 until MAX_PERIODS).map { i ->
            val start = MIN_ANCHOR_DATE.plusDays(i * 10L)
            // 마지막 구간도 닫아 두어 `null`(4자)보다 긴 날짜 문자열을 쓴다.
            val end = if (i == MAX_PERIODS - 1) MAX_ANCHOR_DATE else start.plusDays(5)
            ActivePeriod(start, end)
        }
        val subs = (1..MAX_SUBSCRIPTIONS).map { i ->
            Subscription(
                id = SubscriptionId(i.toLong()),
                name = name,
                price = Money(1_000_000_000_000L, CurrencyCode.KRW),
                cycle = BillingCycle.Months(365),
                anchorDate = MIN_ANCHOR_DATE,
                category = Category.INSURANCE,
                colorKey = ColorKey.VIOLET,
                memo = memo,
                reminder = Reminder(true, 7, LocalTime.of(23, 59)),
                periods = periods
            )
        }
        val snapshot = BackupSnapshot(subs, UserSettings.DEFAULT)

        val encoded = codec.encode(snapshot)

        println(
            "MEASURED largest valid backup length = ${encoded.length} chars (limit $MAX_BACKUP_CHARS)"
        )
        assertThat(encoded.length).isAtMost(MAX_BACKUP_CHARS)
        assertThat(codec.decode(encoded)).isEqualTo(ok(snapshot))
    }

    @Test
    fun escapedMemoAndEmojiNameRoundTrip() {
        val s = sub(1, "😀".repeat(NAME_MAX_LENGTH)).copy(memo = "\"quote\" \\ \n\t\u0000 끝")
        val snapshot = BackupSnapshot(listOf(s), UserSettings.DEFAULT)
        assertThat(codec.decode(codec.encode(snapshot))).isEqualTo(ok(snapshot))
    }

    @Test
    fun thirtyEmojiNameIsValidAndThirtyOneIsMalformed() {
        fun decodeWithName(name: String) = codec.decode(BackupJsonFixture.valid(name = name))
        assertThat(decodeWithName("😀".repeat(30))).isInstanceOf(Outcome.Ok::class.java)
        assertThat(decodeWithName("😀".repeat(31))).isEqualTo(Outcome.Err(BackupError.Malformed))
    }

    @Test
    fun nameIsTrimmedOnDecode() {
        val result = codec.decode(
            BackupJsonFixture.valid(name = "  Netflix  ")
        ) as Outcome.Ok<BackupSnapshot>
        assertThat(result.value.subscriptions.single().name).isEqualTo("Netflix")
    }
}
