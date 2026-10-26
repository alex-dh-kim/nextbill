package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.ImportStaging
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.model.Outcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * 앱 시작 상태 전이(설계 문서 ADR-4·7): `Recovering → Ready | RecoveryFailed`. 시작할 때 남은 가져오기
 * staging이 있으면 이어서 적용해 "전부 이전" 또는 "전부 새것"으로 수렴시킵니다.
 * 취소 외의 예외는 밖으로 내보내지 않습니다(처리기 없는 launch의 예외는 앱을 종료시킴).
 */
@Singleton
class AppStartup @Inject constructor(
    private val coordinator: DataCoordinator,
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val codec: BackupCodec,
    private val staging: ImportStaging,
    private val sync: SyncRemindersUseCase,
    private val refresher: DerivedStateRefresher,
    private val reporter: FailureReporter
) {
    val state: StateFlow<StartupState> = coordinator.state

    /** 바뀌면 앱 셸이 모든 탭 스택을 루트로 되돌립니다. */
    val dataGeneration: StateFlow<Long> = coordinator.generation

    /** Application.onCreate에서 1회. */
    suspend fun start() {
        coordinator.recovery {
            withContext(NonCancellable) {
                runCatchingNonCancellation { recover() }
                    .onFailure {
                        reporter.report("startup recovery", it)
                        setState(StartupState.RecoveryFailed) // staging은 그대로
                    }
            }
            // 실패해도 Ready를 유지합니다. 알람은 다음 sync가 다시 시도합니다.
            if (coordinator.state.value == StartupState.Ready) {
                runCatchingNonCancellation { sync() }
                    .onFailure { reporter.report("startup sync", it) }
            }
        }
        refresher.request()
    }

    suspend fun retryRecovery() {
        coordinator.recovery { setState(StartupState.Recovering) }
        start()
    }

    /**
     * "가져오기 취소": staging만 지웁니다. 일부만 교체됐을 수 있으므로 세대를 먼저 올려 열린 화면을
     * 정리합니다. 삭제가 실패하면 RecoveryFailed 그대로이고, 세대만 하나 더 올라갈 수 있습니다.
     */
    suspend fun discardPendingImport() {
        coordinator.recovery {
            runCatchingNonCancellation {
                val revision = repo.bumpRevision()
                staging.clear()
                revision
            }.onSuccess { revision ->
                setGeneration(revision)
                setState(StartupState.Ready)
                runCatchingNonCancellation { sync() }
                    .onFailure { reporter.report("discard import sync", it) }
            }.onFailure { reporter.report("discard import", it) }
        }
        refresher.request()
    }

    private suspend fun RecoveryScope.recover() {
        setGeneration(repo.revision())
        val json = staging.read()
        if (json == null) {
            setState(StartupState.Ready)
            return
        }
        when (val decoded = codec.decode(json)) {
            is Outcome.Err -> staging.clear()

            is Outcome.Ok -> {
                val revision = applySnapshot(repo, settings, decoded.value)
                staging.clear()
                setGeneration(revision)
            }
        }
        setState(StartupState.Ready)
    }
}
