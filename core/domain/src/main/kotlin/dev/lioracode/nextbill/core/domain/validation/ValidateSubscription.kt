package dev.lioracode.nextbill.core.domain.validation

import dev.lioracode.nextbill.core.domain.billing.newPeriods
import dev.lioracode.nextbill.core.domain.billing.withAnchor
import dev.lioracode.nextbill.core.model.BillingCycle
import dev.lioracode.nextbill.core.model.MAX_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.MIN_ANCHOR_DATE
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Outcome.Err
import dev.lioracode.nextbill.core.model.Outcome.Ok
import dev.lioracode.nextbill.core.model.Subscription
import java.math.BigDecimal

/**
 * 모든 필드를 검사해 오류를 한꺼번에 모으고, 오류가 없을 때만 [Subscription]을 만듭니다.
 * [existing]이 있으면 `existing.withAnchor(anchorDate)`의 사용 구간을 씁니다(설계 문서 §4.2).
 * [existing]은 ACTIVE여야 하며, 아니면 `withAnchor`의 `IllegalArgumentException`이 그대로 전달됩니다.
 * 지원하지 않는 통화는 `IllegalArgumentException`입니다.
 */
fun SubscriptionDraft.validate(existing: Subscription?): Outcome<Set<FieldError>, Subscription> {
    val errors = linkedSetOf<FieldError>()
    val trimmedName = name.trim()
    when {
        trimmedName.isEmpty() -> errors += FieldError.NAME_BLANK
        trimmedName.charCount() > NAME_MAX_LENGTH -> errors += FieldError.NAME_TOO_LONG
    }
    val minor = when (val amount = parseAmount(amountText, currency)) {
        is Ok -> amount.value

        is Err -> {
            errors += amount.error
            null
        }
    }
    if (interval !in BillingCycle.INTERVAL_RANGE) errors += FieldError.INTERVAL_INVALID
    if (memo.charCount() > MEMO_MAX_LENGTH) errors += FieldError.MEMO_TOO_LONG
    if (anchorDate !in MIN_ANCHOR_DATE..MAX_ANCHOR_DATE) errors += FieldError.ANCHOR_OUT_OF_RANGE
    if (errors.isNotEmpty() || minor == null) return Err(errors)

    val periods = existing?.withAnchor(anchorDate)?.periods ?: newPeriods(anchorDate)
    val base = Subscription(
        id = id,
        name = trimmedName,
        price = Money(minor, currency),
        cycle = unit.toCycle(interval),
        anchorDate = anchorDate,
        category = category,
        colorKey = colorKey,
        memo = memo,
        reminder = reminder,
        periods = periods
    )
    return Ok(base)
}

/** 편집 화면용 초안으로 되돌립니다. 금액은 쉼표 없는 일반 표기입니다. */
fun Subscription.toDraft(): SubscriptionDraft = SubscriptionDraft(
    id = id,
    name = name,
    amountText = BigDecimal.valueOf(price.minor, fractionDigits(price.currency)).toPlainString(),
    currency = price.currency,
    interval = cycle.interval,
    unit = cycle.unit,
    anchorDate = anchorDate,
    category = category,
    colorKey = colorKey,
    memo = memo,
    reminder = reminder
)

/** 사용자에게 보이는 글자 수 한도에 쓰는 유니코드 코드 포인트 수(UTF-16 단위 아님). */
fun String.charCount(): Int = codePointCount(0, length)
