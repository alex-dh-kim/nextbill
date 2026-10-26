@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.TestSubscriptions.phoneBill
import dev.lioracode.nextbill.core.testing.TestSubscriptions.youtube
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

private val darkSettings = UserSettings.DEFAULT.copy(themeMode = ThemeMode.DARK)
private val phoneOnly = BackupSnapshot(listOf(phoneBill), darkSettings)

/** 세대 영속: 프로세스 재생성 = 같은 가짜 저장소로 coordinator·startup을 새로 만듦. */
class GenerationPersistenceTest {
    @Test fun editorOpenedAfterImportSavesAfterRecreation() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        env.prepare(BackupSnapshot(listOf(netflix), UserSettings.DEFAULT))
        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))
        val generation = env.coordinator.generation.value
        val opened = env.repo.getAll().single()
        assertThat(generation).isEqualTo(1)

        env.recreate()
        env.startup.start()

        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.save(opened.toDraft().copy(name = "Edited"), generation))
            .isEqualTo(Outcome.Ok(opened.id))
    }

    @Test fun preImportEditorRestoredAfterDeathIsStale() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        val draft = youtube.newDraft()
        val openedAt = env.coordinator.generation.value
        env.prepare(phoneOnly)
        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))

        env.recreate()
        env.startup.start()

        assertThat(env.save(draft, openedAt)).isEqualTo(Outcome.Err(SaveError.Missing))
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
    }

    @Test fun stagingRecoveryInvalidatesOldDraft() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        val draft = youtube.newDraft()
        val openedAt = env.coordinator.generation.value
        // staging.write만 끝나고 프로세스가 죽었습니다.
        env.prepare(phoneOnly)
        env.staging.content = "fake-backup:1"

        env.recreate()
        env.startup.start()

        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.save(draft, openedAt)).isEqualTo(Outcome.Err(SaveError.Missing))
    }
}

class ImportBackupUseCaseTest {
    private suspend fun readyEnv(scope: CoroutineScope): TestEnv {
        val env = TestEnv(scope, listOf(netflix))
        env.startup.start()
        return env
    }

    @Test fun importMalformedLeavesEverythingUntouched() = runTest {
        val env = readyEnv(appScope())
        env.codec.decodeResult = Outcome.Err(BackupError.Malformed)

        assertThat(
            env.import("{")
        ).isEqualTo(Outcome.Err(ImportError.Invalid(BackupError.Malformed)))

        assertThat(env.staging.read()).isNull()
        assertThat(env.repo.getAll()).containsExactly(netflix)
        assertThat(env.coordinator.generation.value).isEqualTo(0)
    }

    @Test fun importUnsupportedVersion() = runTest {
        val env = readyEnv(appScope())
        env.codec.decodeResult = Outcome.Err(BackupError.UnsupportedVersion(2))

        assertThat(env.import("{}"))
            .isEqualTo(Outcome.Err(ImportError.Invalid(BackupError.UnsupportedVersion(2))))
    }

    @Test fun importReplacesSettingsAndSubscriptions() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)

        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))

        assertThat(env.settings.current).isEqualTo(darkSettings)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(env.staging.read()).isNull()
        assertThat(env.codec.lastEncoded).isEqualTo(phoneOnly)
    }

    @Test fun importRetriesApplyOnce() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        env.repo.failNextReplaceAll = IOException("busy")

        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))
        assertThat(env.staging.read()).isNull()
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
    }

    @Test fun importApplyFailsTwiceMarksRecoveryFailed() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        env.repo.failNextReplaceAll = IOException("busy")
        env.repo.failNextReplaceAllTimes = 2

        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        assertThat(env.staging.read()).isNotNull()
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.RecoveryFailed)
    }

    @Test fun importWhileNotReadyIsRejected() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.prepare(phoneOnly)

        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.NotReady))
        assertThat(env.staging.read()).isNull()
        assertThat(env.repo.getAll()).containsExactly(netflix)
    }

    @Test fun stagingWriteFailureChangesNothing() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        env.staging.failWrite = IOException("rename failed")

        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.StagingFailed))

        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.repo.getAll()).containsExactly(netflix)
        assertThat(env.settings.current).isEqualTo(UserSettings.DEFAULT)
        assertThat(env.staging.read()).isNull()
        assertThat(env.coordinator.generation.value).isEqualTo(0)
    }

    @Test fun clearFailureAfterApplyMarksRecoveryFailed() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        env.staging.failClear = IOException("delete failed")

        assertThat(env.import("json")).isEqualTo(Outcome.Err(ImportError.ApplyFailed))
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.RecoveryFailed)
        assertThat(env.staging.read()).isNotNull()

        env.staging.failClear = null
        env.startup.retryRecovery()

        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(env.settings.current).isEqualTo(darkSettings)
        assertThat(env.staging.read()).isNull()
    }

    @Test fun cancelDuringImportStillFinalizes() = runTest {
        val env = readyEnv(appScope())
        advanceUntilIdle()
        val dayChanges = env.dayChange.count
        env.prepare(phoneOnly)
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()

        importing.cancel()
        gate.complete(Unit)
        val outcome = runCatching { importing.await() }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(env.staging.read()).isNull()
        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.coordinator.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        advanceUntilIdle()
        assertThat(env.dayChange.count).isEqualTo(dayChanges + 1)
    }

    @Test fun okOnlyAfterFinalize() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)

        val result = env.import("json")
        val stagingAtOk = env.staging.read()
        val generationAtOk = env.coordinator.generation.value

        assertThat(result).isEqualTo(Outcome.Ok(1))
        assertThat(stagingAtOk).isNull()
        assertThat(generationAtOk).isEqualTo(1)
    }

    @Test fun importBumpsGeneration() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        assertThat(env.repo.revision()).isEqualTo(0)

        env.import("json")

        assertThat(env.repo.revision()).isEqualTo(1)
        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.startup.dataGeneration.value).isEqualTo(1)
    }

    @Test fun alarmFailureAfterImportIsStillOk() = runTest {
        val env = readyEnv(appScope())
        advanceUntilIdle()
        val sets = env.alarm.setCount
        env.prepare(phoneOnly)
        env.alarm.failNext = SecurityException("alarm denied")

        assertThat(env.import("json")).isEqualTo(Outcome.Ok(1))
        assertThat(env.staging.read()).isNull()
        assertThat(env.coordinator.generation.value).isEqualTo(1)
        assertThat(env.alarm.setCount).isEqualTo(sets)

        advanceUntilIdle()

        assertThat(env.alarm.setCount).isEqualTo(sets + 1)
        assertThat(env.alarm.nextAt).isNotNull()
    }

    @Test fun exportEncodesCurrentState() = runTest {
        val env = TestEnv(
            appScope(),
            listOf(netflix, phoneBill),
            initialSettings = darkSettings
        )
        env.startup.start()

        assertThat(env.export()).isEqualTo(Outcome.Ok("fake-backup:2"))
        assertThat(env.codec.lastEncoded)
            .isEqualTo(BackupSnapshot(listOf(netflix, phoneBill), darkSettings))
    }

    @Test fun exportWhileNotReadyIsRejected() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))

        assertThat(env.export()).isEqualTo(Outcome.Err(ExportError.NotReady))
        assertThat(env.codec.lastEncoded).isNull()
    }

    @Test fun exportReadFailureIsReported() = runTest {
        val env = readyEnv(appScope())
        env.repo.failNextGetAll = IOException("db")

        assertThat(env.export()).isEqualTo(Outcome.Err(ExportError.ReadFailed))
        assertThat(env.codec.lastEncoded).isNull()
    }

    @Test fun exportDuringImportSeesOnlyNewData() = runTest {
        val env = readyEnv(appScope())
        env.prepare(phoneOnly)
        val gate = CompletableDeferred<Unit>()
        env.repo.replaceAllGate = gate
        val importing = async { env.import("json") }
        env.repo.replaceAllEntered.await()

        // 가져오기가 구독만 바꾸고 설정은 아직인 시점에 내보내기를 시작합니다.
        val exporting = async { env.export() }
        advanceUntilIdle()
        assertThat(exporting.isCompleted).isFalse()
        gate.complete(Unit)

        assertThat(importing.await()).isEqualTo(Outcome.Ok(1))
        assertThat(exporting.await()).isEqualTo(Outcome.Ok("fake-backup:1"))
        val exported = env.codec.lastEncoded!!
        assertThat(exported.subscriptions.withoutIds())
            .isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(exported.settings).isEqualTo(darkSettings)
    }
}

class AppStartupTest {
    @Test fun startupAlarmFailureStaysReady() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.alarm.failNext = SecurityException("alarm denied")

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.alarm.nextAt).isNull()
    }

    @Test fun startupStorageExceptionDoesNotEscape() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.repo.failNextRevision = IOException("db")

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.RecoveryFailed)
        assertThat(env.failures.map { it.first }).containsExactly("startup recovery")
    }

    /** 첫 sync의 실패(예: 기기에 없는 API의 NoSuchMethodError)도 삼키되 알립니다. */
    @Test fun startupSyncFailureIsReported() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.scheduled.failNextPut = NoSuchMethodError("LocalDate.ofInstant")

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.failures.first().first).isEqualTo("startup sync")
        assertThat(env.failures.first().second).isInstanceOf(NoSuchMethodError::class.java)
    }

    @Test fun startupRecoveringThenReady() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.prepare(phoneOnly)
        env.staging.content = "fake-backup:1"
        assertThat(env.startup.state.value).isEqualTo(StartupState.Recovering)

        env.startup.start()
        advanceUntilIdle()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        val phone = env.repo.getAll().single()
        assertThat(listOf(phone).withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(env.settings.current).isEqualTo(darkSettings)
        assertThat(env.staging.read()).isNull()
        assertThat(env.scheduled.snapshot.keys).containsExactly(phone.id)
        assertThat(env.alarm.setCount).isEqualTo(1)
    }

    @Test fun startupRecoveryFailedThenRetry() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(phoneOnly)
        assertThat(env.staging.read()).isNotNull()

        env.startup.retryRecovery()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(env.staging.read()).isNull()
    }

    @Test fun startupClearFailureIsRecoveryFailed() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.prepare(phoneOnly)
        env.staging.content = "fake-backup:1"
        env.staging.failClear = IOException("delete failed")

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.RecoveryFailed)
        assertThat(env.staging.read()).isNotNull()
    }

    @Test fun malformedStagingIsDiscarded() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.codec.decodeResult = Outcome.Err(BackupError.Malformed)
        env.staging.content = "garbage"

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.staging.read()).isNull()
        assertThat(env.repo.getAll()).containsExactly(netflix)
    }

    @Test fun discardPendingImport() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(phoneOnly)
        val before = env.coordinator.generation.value

        env.startup.discardPendingImport()

        assertThat(env.staging.read()).isNull()
        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.coordinator.generation.value).isEqualTo(before + 1)
        assertThat(env.repo.revision()).isEqualTo(before + 1)
        assertThat(env.scheduled.snapshot).containsKey(netflix.id)
    }

    @Test fun discardClearFailureStaysFailed() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startIntoRecoveryFailed(phoneOnly)
        val before = env.coordinator.generation.value
        env.staging.failClear = IOException("delete failed")

        env.startup.discardPendingImport()

        assertThat(env.startup.state.value).isEqualTo(StartupState.RecoveryFailed)
        assertThat(env.staging.read()).isNotNull()
        assertThat(env.coordinator.generation.value).isEqualTo(before)
    }

    @Test fun crashAfterStagingConvergesOnRecovery() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        // staging.write만 된 상태(apply 전)에서 죽은 뒤의 새 프로세스.
        env.prepare(phoneOnly)
        env.staging.content = "fake-backup:1"

        env.startup.start()

        assertThat(env.repo.getAll().withoutIds()).isEqualTo(phoneOnly.subscriptions.withoutIds())
        assertThat(env.settings.current).isEqualTo(darkSettings)
        assertThat(env.staging.read()).isNull()
        assertThat(env.coordinator.generation.value).isEqualTo(1)
    }

    @Test fun startupWithoutStagingIsReady() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))

        env.startup.start()

        assertThat(env.startup.state.value).isEqualTo(StartupState.Ready)
        assertThat(env.coordinator.generation.value).isEqualTo(0)
        assertThat(env.repo.replaceAllCount).isEqualTo(0)
        assertThat(env.repo.getAll()).containsExactly(netflix)
    }
}
