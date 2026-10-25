package dev.lioracode.nextbill.feature.detail.impl

import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.Money
import dev.lioracode.nextbill.core.model.Subscription
import java.time.LocalDate

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Content(
        val subscription: Subscription,
        val mode: DetailMode,
        /** 다가오는 결제일 3개. 해지한 구독이면 빈 목록. */
        val upcoming: List<Pair<LocalDate, DueLabel>>,
        val monthly: Money,
        val yearly: Money,
        val busy: Boolean,
        val message: DetailMessage?
    ) : DetailUiState

    /** 끝 상태. 이 화면에서 해지·삭제를 마쳤거나 구독이 사라졌습니다. 화면은 `navigator.pop(key)`를 부릅니다(멱등). */
    data object Closed : DetailUiState
}

sealed interface DetailMode {
    data object Active : DetailMode

    /** 첫 결제 전: 해지할 수 없고 삭제만 됩니다. */
    data object BeforeFirstBilling : DetailMode

    /** 읽기 전용. */
    data class Cancelled(val cancelledAt: LocalDate) : DetailMode
}

/** MISSING은 메시지가 아니라 [DetailUiState.Closed]로 표현합니다. */
enum class DetailMessage { PERIOD_LIMIT, NOT_READY, STORAGE_FAILED }
