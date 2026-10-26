package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Outcome
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface ExportError {
    /** 복구 중이거나 복구에 실패한 상태라 읽지 않았습니다. */
    data object NotReady : ExportError

    /** 저장소를 읽지 못했습니다. */
    data object ReadFailed : ExportError
}

/**
 * 구독(사용 구간 포함)과 설정 전체를 백업 문자열로 만듭니다(ADR-7). 가져오기와 같은 잠금 안에서 둘을
 * 함께 읽으므로, 가져오기 도중에 내보내도 이전 데이터와 새 데이터가 섞이지 않습니다. 인코딩은 잠금 밖에서 합니다.
 */
class ExportBackupUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val codec: BackupCodec,
    private val coordinator: DataCoordinator
) {
    suspend operator fun invoke(): Outcome<ExportError, String> = when (
        val admission = coordinator.write {
            runCatchingNonCancellation { BackupSnapshot(repo.getAll(), settings.settings.first()) }
        }
    ) {
        Admission.NotReady, Admission.Stale -> Outcome.Err(ExportError.NotReady)

        is Admission.Ran -> admission.value.fold(
            onSuccess = { Outcome.Ok(codec.encode(it)) },
            onFailure = { Outcome.Err(ExportError.ReadFailed) }
        )
    }
}
