package dev.lioracode.nextbill.core.data.backup

import dev.lioracode.nextbill.core.domain.validation.MAX_MINOR
import dev.lioracode.nextbill.core.domain.validation.MEMO_MAX_LENGTH
import dev.lioracode.nextbill.core.domain.validation.NAME_MAX_LENGTH
import dev.lioracode.nextbill.core.domain.validation.charCount
import dev.lioracode.nextbill.core.domain.validation.toCycle
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.MAX_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.MAX_PERIODS
import dev.lioracode.nextbill.core.model.MAX_SUBSCRIPTIONS
import dev.lioracode.nextbill.core.model.MIN_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.UserSettings
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

internal class InvalidBackupException(message: String) : Exception(message)

internal val BACKUP_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "HH:mm"
).withResolverStyle(ResolverStyle.STRICT)

/**
 * [BackupDto]를 검사하면서 도메인 모델로 바꿉니다. 위반은 모두 [InvalidBackupException]입니다.
 * 구간 불변식은 `Subscription` init의 `require`가 검사하고, 그 `IllegalArgumentException`도 같은 예외로 바꿉니다.
 */
internal object BackupValidation {
    fun toSnapshot(dto: BackupDto): Pair<List<Subscription>, UserSettings> {
        invalidIf(dto.subscriptions.size > MAX_SUBSCRIPTIONS) { "too many subscriptions" }
        val subscriptions = dto.subscriptions.map(::toSubscription)
        return subscriptions to toSettings(dto.settings)
    }

    private fun toSubscription(dto: SubscriptionDto): Subscription {
        val name = dto.name.trim()
        invalidIf(name.isEmpty() || name.charCount() > NAME_MAX_LENGTH) { "name length" }
        invalidIf(dto.memo.charCount() > MEMO_MAX_LENGTH) { "memo too long" }
        invalidIf(dto.priceMinor !in 1..MAX_MINOR) { "price out of range" }
        invalidIf(dto.periods.size > MAX_PERIODS) { "too many periods" }
        val anchor = parseDate(dto.anchorDate)
        val periods = dto.periods.map {
            ActivePeriod(parseDate(it.start), it.end?.let(::parseDate))
        }
        return try {
            Subscription(
                id = SubscriptionId(0),
                name = name,
                price = Money(dto.priceMinor, parseCurrency(dto.currency)),
                cycle = dto.cycle.unit.toCycle(dto.cycle.interval),
                anchorDate = anchor,
                category = dto.category,
                colorKey = dto.colorKey,
                memo = dto.memo,
                reminder = toReminder(dto.reminder),
                periods = periods
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException(e.message ?: "invalid subscription")
        }
    }

    private fun toSettings(dto: SettingsDto) = UserSettings(
        defaultReminder = toReminder(dto.defaultReminder),
        themeMode = dto.themeMode,
        useDynamicColor = dto.useDynamicColor,
        defaultCurrency = parseCurrency(dto.defaultCurrency)
    )

    private fun toReminder(dto: ReminderDto): Reminder {
        invalidIf(dto.daysBefore !in Reminder.DAYS_BEFORE_OPTIONS) { "daysBefore" }
        val time = try {
            LocalTime.parse(dto.time, BACKUP_TIME_FORMAT)
        } catch (e: DateTimeException) {
            throw InvalidBackupException("time: ${e.message}")
        }
        return Reminder(dto.enabled, dto.daysBefore, time)
    }

    private fun parseCurrency(code: String): CurrencyCode {
        val currency = CurrencyCode(code)
        invalidIf(currency !in CurrencyCode.SUPPORTED) { "unsupported currency" }
        return currency
    }

    private fun parseDate(text: String): LocalDate {
        val date = try {
            LocalDate.parse(text)
        } catch (e: DateTimeException) {
            throw InvalidBackupException("date: ${e.message}")
        }
        invalidIf(date !in MIN_ANCHOR_DATE..MAX_ANCHOR_DATE) { "date out of range" }
        return date
    }

    private inline fun invalidIf(condition: Boolean, message: () -> String) {
        if (condition) throw InvalidBackupException(message())
    }
}
