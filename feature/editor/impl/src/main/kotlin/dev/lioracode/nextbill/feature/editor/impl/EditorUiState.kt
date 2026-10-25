package dev.lioracode.nextbill.feature.editor.impl

import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.model.SubscriptionId

/**
 * 편집 화면 상태. [errors]는 지금 입력의 검증 결과이고, 화면은 [showErrors]가 true일 때만 보여 줍니다
 * (처음 저장을 누르기 전에는 오류를 보이지 않음).
 */
data class EditorUiState(
    val isNew: Boolean,
    val draft: SubscriptionDraft,
    val errors: Set<FieldError>,
    val showErrors: Boolean,
    val phase: EditorPhase,
    val message: EditorMessage?
)

/** 저장 실패 안내. 화면이 보여 준 뒤 `messageShown()`을 부릅니다. */
enum class EditorMessage { LIMIT_REACHED, NOT_READY, STORAGE_FAILED }

sealed interface EditorPhase {
    data object Editing : EditorPhase

    /** 알림 권한 안내 시트를 보여 줍니다. [saveAfter]면 시트를 닫은 뒤 저장합니다. */
    data class AskingPermission(val saveAfter: Boolean) : EditorPhase

    data object Saving : EditorPhase

    /** 화면은 이 상태를 보고 `navigator.pop(key)`를 부릅니다(여러 번 불러도 한 번만 적용). */
    data class Saved(val id: SubscriptionId) : EditorPhase

    /** 수정하려던 구독이 사라졌습니다(삭제·가져오기). 화면은 닫힙니다. */
    data object Missing : EditorPhase
}
