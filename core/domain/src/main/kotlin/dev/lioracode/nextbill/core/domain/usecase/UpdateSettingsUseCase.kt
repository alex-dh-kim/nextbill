package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.UserSettings
import javax.inject.Inject

/** 설정 쓰기도 같은 관문을 지납니다(복구 중·복구 실패 뒤에는 NOT_READY). */
class UpdateSettingsUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val coordinator: DataCoordinator
) {
    suspend operator fun invoke(
        transform: (UserSettings) -> UserSettings
    ): Outcome<ActionError, Unit> = when (
        val admission = coordinator.write {
            runCatchingNonCancellation { settings.update(transform) }
        }
    ) {
        Admission.NotReady, Admission.Stale -> Outcome.Err(ActionError.NOT_READY)

        is Admission.Ran -> admission.value.fold(
            onSuccess = { Outcome.Ok(Unit) },
            onFailure = { Outcome.Err(ActionError.STORAGE_FAILED) }
        )
    }
}
