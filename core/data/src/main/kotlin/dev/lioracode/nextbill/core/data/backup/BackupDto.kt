package dev.lioracode.nextbill.core.data.backup

import dev.lioracode.nextbill.core.domain.validation.CycleUnit
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.ThemeMode
import kotlinx.serialization.Serializable

const val CURRENT_SCHEMA_VERSION = 1

/** 파일 형식(v1). 날짜·시각은 ISO 문자열로 읽고 [BackupValidation]에서 검사합니다. id는 쓰지 않습니다. */
@Serializable
internal data class BackupDto(
    val schemaVersion: Int,
    val exportedAt: String,
    val subscriptions: List<SubscriptionDto>,
    val settings: SettingsDto
)

@Serializable
internal data class SubscriptionDto(
    val name: String,
    val priceMinor: Long,
    val currency: String,
    val cycle: CycleDto,
    val anchorDate: String,
    val category: Category,
    val colorKey: ColorKey,
    val memo: String,
    val reminder: ReminderDto,
    val periods: List<PeriodDto>
)

@Serializable
internal data class CycleDto(val unit: CycleUnit, val interval: Int)

@Serializable
internal data class ReminderDto(val enabled: Boolean, val daysBefore: Int, val time: String)

@Serializable
internal data class PeriodDto(val start: String, val end: String?)

@Serializable
internal data class SettingsDto(
    val defaultReminder: ReminderDto,
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val defaultCurrency: String
)
