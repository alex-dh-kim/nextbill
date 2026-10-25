package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.model.Outcome

/** 해지·복원·삭제·설정 쓰기의 실패. */
enum class ActionError { NOT_STARTED, PERIOD_LIMIT, MISSING, NOT_READY, STORAGE_FAILED }

/**
 * 해지·복원·삭제의 공통 절차: 관문 안에서 [block]을 실행하고, 성공하면 잠금 밖에서 갱신을 요청합니다.
 * NotReady → NOT_READY, Stale → MISSING, DB 예외(취소 제외) → STORAGE_FAILED.
 */
internal suspend fun DataCoordinator.writeAction(
    generation: Long,
    refresher: DerivedStateRefresher,
    block: suspend () -> Outcome<ActionError, Unit>
): Outcome<ActionError, Unit> {
    val outcome = when (
        val admission = write(generation) {
            runCatchingNonCancellation { block() }
        }
    ) {
        Admission.NotReady -> Outcome.Err(ActionError.NOT_READY)
        Admission.Stale -> Outcome.Err(ActionError.MISSING)
        is Admission.Ran -> admission.value.getOrElse { Outcome.Err(ActionError.STORAGE_FAILED) }
    }
    if (outcome is Outcome.Ok) refresher.request()
    return outcome
}
