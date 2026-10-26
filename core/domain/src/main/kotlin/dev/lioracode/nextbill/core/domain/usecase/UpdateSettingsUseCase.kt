package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.UserSettings
import javax.inject.Inject

/**
 * 설정 쓰기도 같은 관문을 지납니다(복구 중·복구 실패 뒤에는 NOT_READY). 성공하면 잠금 밖에서 갱신을
 * 요청합니다(기본 통화가 바뀌면 위젯 합계도 바뀜).
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val coordinator: DataCoordinator,
    private val refresher: DerivedStateRefresher
) {
    suspend operator fun invoke(
        transform: (UserSettings) -> UserSettings
    ): Outcome<ActionError, Unit> {
        val outcome = when (
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
        if (outcome is Outcome.Ok) refresher.request()
        return outcome
    }
}
