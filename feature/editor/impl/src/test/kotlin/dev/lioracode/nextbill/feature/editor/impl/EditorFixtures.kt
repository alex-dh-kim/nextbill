package dev.lioracode.nextbill.feature.editor.impl

import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.Category
import dev.lioracode.nextbill.core.model.ColorKey
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d

/** 시안 02: 이름은 넷플릭스, 금액은 비어 있고, 저장을 눌러 오류가 보이는 새 구독. */
internal val newWithAmountError = EditorUiState(
    isNew = true,
    draft = SubscriptionDraft(
        name = "넷플릭스",
        currency = CurrencyCode.KRW,
        anchorDate = d("2026-10-15"),
        category = Category.VIDEO,
        colorKey = ColorKey.RED,
        reminder = Reminder.DEFAULT
    ),
    errors = setOf(FieldError.AMOUNT_BLANK),
    showErrors = true,
    phase = EditorPhase.Editing,
    message = null
)

internal val editingExisting = EditorUiState(
    isNew = false,
    draft = TestSubscriptions.youtube.toDraft().copy(memo = "가족 요금제로 바꿀지 고민 중"),
    errors = emptySet(),
    showErrors = false,
    phase = EditorPhase.Editing,
    message = null
)
