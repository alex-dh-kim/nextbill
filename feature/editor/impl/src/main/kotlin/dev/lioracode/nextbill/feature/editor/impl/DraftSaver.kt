package dev.lioracode.nextbill.feature.editor.impl

import dev.lioracode.nextbill.core.domain.validation.CycleUnit
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [SubscriptionDraft]를 SavedStateHandle에 넣을 JSON 문자열로 바꿉니다. 프로세스가 다시 만들어져도
 * 입력이 남습니다. 도메인 모델(JVM 모듈)에 직렬화 애너테이션을 달지 않으려고 화면 전용 DTO를 둡니다.
 */
internal object DraftSaver {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(draft: SubscriptionDraft): String =
        json.encodeToString(SavedDraft.serializer(), draft.toSaved())

    /** 읽을 수 없는 값이면 null(처음부터 다시 엽니다). */
    fun decode(text: String): SubscriptionDraft? = try {
        json.decodeFromString(SavedDraft.serializer(), text).toDraft()
    } catch (e: IllegalArgumentException) {
        // SerializationException과 없는 enum 이름(valueOf)은 IllegalArgumentException 계열입니다.
        null
    } catch (e: java.time.DateTimeException) {
        // 잘못된 날짜·시각 문자열(DateTimeParseException)은 DateTimeException 계열입니다.
        null
    }
}

@Serializable
private data class SavedDraft(
    val id: Long,
    val name: String,
    val amountText: String,
    val currency: String,
    val interval: Int,
    val unit: String,
    val anchorDate: String,
    val category: String,
    val colorKey: String,
    val memo: String,
    val reminderEnabled: Boolean,
    val reminderDaysBefore: Int,
    val reminderTime: String
)

private fun SubscriptionDraft.toSaved() = SavedDraft(
    id = id.value,
    name = name,
    amountText = amountText,
    currency = currency.value,
    interval = interval,
    unit = unit.name,
    anchorDate = anchorDate.toString(),
    category = category.name,
    colorKey = colorKey.name,
    memo = memo,
    reminderEnabled = reminder.enabled,
    reminderDaysBefore = reminder.daysBefore,
    reminderTime = reminder.time.toString()
)

private fun SavedDraft.toDraft() = SubscriptionDraft(
    id = SubscriptionId(id),
    name = name,
    amountText = amountText,
    currency = CurrencyCode(currency),
    interval = interval,
    unit = CycleUnit.valueOf(unit),
    anchorDate = LocalDate.parse(anchorDate),
    category = Category.valueOf(category),
    colorKey = ColorKey.valueOf(colorKey),
    memo = memo,
    reminder = Reminder(reminderEnabled, reminderDaysBefore, LocalTime.parse(reminderTime))
)
