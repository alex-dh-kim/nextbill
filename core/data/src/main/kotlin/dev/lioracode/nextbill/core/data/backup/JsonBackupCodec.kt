package dev.lioracode.nextbill.core.data.backup

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.backup.MAX_BACKUP_CHARS
import dev.lioracode.nextbill.core.domain.validation.unit
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.UserSettings
import java.time.Clock
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** 백업 v1 JSON 코덱. 디코딩은 아무것도 바꾸지 않고, 읽은 구독의 id는 모두 0입니다. */
class JsonBackupCodec @Inject constructor(private val clock: Clock) : BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun encode(snapshot: BackupSnapshot): String = json.encodeToString(
        BackupDto.serializer(),
        BackupDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAt = clock.instant().toString(),
            subscriptions = snapshot.subscriptions.map { it.toDto() },
            settings = snapshot.settings.toDto()
        )
    )

    override fun decode(json: String): Outcome<BackupError, BackupSnapshot> {
        if (json.length > MAX_BACKUP_CHARS) return Outcome.Err(BackupError.TooLarge)
        return try {
            val root = this.json.parseToJsonElement(json) as? JsonObject ?: return malformed()
            val version = (root["schemaVersion"] as? JsonPrimitive)
                ?.takeUnless { it.isString }
                ?.intOrNull
            when {
                version == null || version < 1 -> return malformed()

                version > CURRENT_SCHEMA_VERSION -> return Outcome.Err(
                    BackupError.UnsupportedVersion(version)
                )
            }
            val dto = this.json.decodeFromJsonElement(BackupDto.serializer(), root)
            val (subscriptions, settings) = BackupValidation.toSnapshot(dto)
            Outcome.Ok(BackupSnapshot(subscriptions, settings))
        } catch (_: SerializationException) {
            malformed()
        } catch (_: InvalidBackupException) {
            malformed()
        } catch (_: IllegalArgumentException) {
            malformed()
        }
    }

    private fun malformed(): Outcome<BackupError, BackupSnapshot> =
        Outcome.Err(BackupError.Malformed)
}

private fun Subscription.toDto() = SubscriptionDto(
    name = name,
    priceMinor = price.minor,
    currency = price.currency.value,
    cycle = CycleDto(cycle.unit, cycle.interval),
    anchorDate = anchorDate.toString(),
    category = category,
    colorKey = colorKey,
    memo = memo,
    reminder = reminder.toDto(),
    periods = periods.map { PeriodDto(it.start.toString(), it.end?.toString()) }
)

private fun Reminder.toDto() = ReminderDto(enabled, daysBefore, BACKUP_TIME_FORMAT.format(time))

private fun UserSettings.toDto() = SettingsDto(
    defaultReminder = defaultReminder.toDto(),
    themeMode = themeMode,
    useDynamicColor = useDynamicColor,
    defaultCurrency = defaultCurrency.value
)
