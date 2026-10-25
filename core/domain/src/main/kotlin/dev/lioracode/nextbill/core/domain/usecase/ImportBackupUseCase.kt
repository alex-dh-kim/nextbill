package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.backup.ImportStaging
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Outcome
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface ImportError {
    data class Invalid(val error: BackupError) : ImportError

    data object NotReady : ImportError

    /** staging 쓰기 실패. 아무것도 바뀌지 않았습니다. */
    data object StagingFailed : ImportError

    /** 적용(2회) 또는 staging 삭제 실패. 앱은 RecoveryFailed입니다. */
    data object ApplyFailed : ImportError
}

/**
 * 전체 교체 가져오기(설계 문서 ADR-7). staging에 먼저 쓰고 적용한 뒤 지웁니다. staging 쓰기부터
 * 세대 갱신·sync까지는 코루틴 취소로 끊기지 않습니다. `Ok`는 staging 삭제와 세대 갱신이 끝난 뒤에만
 * 돌려줍니다. 그 뒤의 sync 실패는 결과를 바꾸지 않습니다(ADR-13).
 */
class ImportBackupUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val codec: BackupCodec,
    private val staging: ImportStaging,
    private val coordinator: DataCoordinator,
    private val sync: SyncRemindersUseCase,
    private val refresher: DerivedStateRefresher
) {
    suspend operator fun invoke(json: String): Outcome<ImportError, Int> {
        val snapshot = when (val decoded = codec.decode(json)) {
            is Outcome.Err -> return Outcome.Err(ImportError.Invalid(decoded.error))
            is Outcome.Ok -> decoded.value
        }
        val encoded = codec.encode(snapshot)
        // 저장·설정·알림 처리는 이 블록이 끝날 때까지 기다리고, 끝난 뒤 상태·세대를 다시 검사합니다.
        val result = try {
            coordinator.recovery {
                if (coordinator.state.value != StartupState.Ready) {
                    Outcome.Err(ImportError.NotReady)
                } else {
                    withContext(NonCancellable) { stageAndApply(snapshot, encoded) }
                }
            }
        } finally {
            // 취소된 가져오기도 마무리했으므로 파생 상태 갱신은 요청합니다.
            refresher.request()
        }
        // 취소된 가져오기도 위에서 끝까지 마무리했지만, 결과는 Ok가 아니라 취소로 끝냅니다.
        currentCoroutineContext().ensureActive()
        return result
    }

    private suspend fun RecoveryScope.stageAndApply(
        snapshot: BackupSnapshot,
        encoded: String
    ): Outcome<ImportError, Int> {
        // rename 전 실패라 아무것도 바뀌지 않았습니다.
        runCatchingNonCancellation { staging.write(encoded) }
            .onFailure { return Outcome.Err(ImportError.StagingFailed) }
        val revision = runCatchingNonCancellation { applySnapshot(repo, settings, snapshot) }
            .getOrElse {
                setState(StartupState.RecoveryFailed)
                return Outcome.Err(ImportError.ApplyFailed)
            }
        // 데이터는 새것입니다. 다시 시도하면 같은 내용을 다시 적용하고 삭제합니다.
        runCatchingNonCancellation { staging.clear() }
            .onFailure {
                setState(StartupState.RecoveryFailed)
                return Outcome.Err(ImportError.ApplyFailed)
            }
        // 열려 있던 화면은 루트로, 기다리던 쓰기는 Stale.
        setGeneration(revision)
        runCatchingNonCancellation { sync() }
        return Outcome.Ok(snapshot.subscriptions.size)
    }
}

/**
 * 스냅샷을 적용하고 새 데이터 세대를 돌려줍니다. 실패하면 한 번 다시 시도하고, 그것도 실패하면 던집니다.
 * 세대는 `replaceAll`과 같은 트랜잭션에서 오릅니다.
 */
internal suspend fun applySnapshot(
    repo: SubscriptionRepository,
    settings: SettingsRepository,
    snapshot: BackupSnapshot
): Long {
    suspend fun once(): Long {
        val revision = repo.replaceAll(snapshot.subscriptions)
        settings.replace(snapshot.settings)
        return revision
    }
    return runCatchingNonCancellation { once() }.getOrElse { once() }
}
