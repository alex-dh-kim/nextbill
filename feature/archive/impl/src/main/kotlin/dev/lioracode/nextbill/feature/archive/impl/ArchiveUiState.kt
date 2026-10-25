package dev.lioracode.nextbill.feature.archive.impl

import dev.lioracode.nextbill.core.model.Subscription

sealed interface ArchiveUiState {
    data object Loading : ArchiveUiState

    /** [items]는 해지일 최신순. 빈 목록이면 빈 상태 화면. */
    data class Ready(val items: List<Subscription>, val message: ArchiveMessage?) : ArchiveUiState
}

/** 스낵바로 한 번 보여 준 뒤 `messageShown()`으로 지웁니다. */
enum class ArchiveMessage { RESTORED, PERIOD_LIMIT, NOT_READY, STORAGE_FAILED, MISSING }
