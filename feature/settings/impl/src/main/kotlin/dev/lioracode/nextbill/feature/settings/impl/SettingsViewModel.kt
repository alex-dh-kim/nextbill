package dev.lioracode.nextbill.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.IoDispatcher
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.domain.usecase.ActionError
import dev.lioracode.nextbill.core.domain.usecase.ExportBackupUseCase
import dev.lioracode.nextbill.core.domain.usecase.ExportError
import dev.lioracode.nextbill.core.domain.usecase.ImportBackupUseCase
import dev.lioracode.nextbill.core.domain.usecase.ImportError
import dev.lioracode.nextbill.core.domain.usecase.UpdateSettingsUseCase
import dev.lioracode.nextbill.core.domain.usecase.runCatchingNonCancellation
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Reminder
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.feature.settings.impl.backup.BackupTooLargeException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 설정(F10)과 백업·복원(F9). 설정 쓰기는 [UpdateSettingsUseCase]의 관문을 지나고, 백업 파일 읽기·쓰기는
 * 화면이 넘긴 함수를 IO 디스패처에서 실행합니다. 화면 이동은 하지 않습니다(화면이 Navigator로).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    @Named("appVersion") private val appVersion: String,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val dateSource: CurrentDateSource
) : ViewModel() {
    private data class Local(
        val backup: BackupStatus = BackupStatus.IDLE,
        val message: SettingsMessage? = null
    )

    private val local = MutableStateFlow(Local())

    /** null = 설정을 읽는 중. */
    val uiState: StateFlow<SettingsUiState?> =
        combine(settingsRepository.settings, local) { settings, l ->
            SettingsUiState(settings, appVersion, l.backup, l.message)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** OS 권한 대화상자를 띄운 적이 있는지(알림 수신 가능 여부 계산용). */
    val permissionRequestedBefore: StateFlow<Boolean> =
        settingsRepository.notificationPermissionRequested
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** 내보내기 파일 이름 제안: `nextbill-backup-2026-11-15.json`. */
    fun backupFileName(): String = "nextbill-backup-${dateSource.today.value}.json"

    fun setDefaultReminder(r: Reminder) = write { it.copy(defaultReminder = r) }

    fun setTheme(m: ThemeMode) = write { it.copy(themeMode = m) }

    fun setDynamicColor(on: Boolean) = write { it.copy(useDynamicColor = on) }

    fun setDefaultCurrency(c: CurrencyCode) = write { it.copy(defaultCurrency = c) }

    /** OS 권한 대화상자의 결과(허용·거부 모두). 요청 이력을 기록합니다. */
    fun onPermissionRequestResult() {
        viewModelScope.launch {
            runCatchingNonCancellation { settingsRepository.markNotificationPermissionRequested() }
        }
    }

    /** [write]는 화면이 고른 문서에 문자열을 씁니다. 실패하면 IOException을 던집니다. */
    fun export(write: suspend (String) -> Unit) = runBackup(BackupStatus.EXPORTING) {
        when (val result = exportBackup()) {
            is Outcome.Err -> when (result.error) {
                ExportError.NotReady -> SettingsMessage.ExportNotReady
                ExportError.ReadFailed -> SettingsMessage.SettingsStorageFailed
            }

            is Outcome.Ok -> try {
                withContext(io) { write(result.value) }
                SettingsMessage.Exported
            } catch (e: IOException) {
                SettingsMessage.IoFailed
            } catch (e: SecurityException) {
                SettingsMessage.IoFailed
            }
        }
    }

    /**
     * [read]는 화면이 고른 문서를 읽습니다. 한도를 넘으면 [BackupTooLargeException], 그 밖의 읽기 실패는
     * IOException을 던집니다.
     */
    fun import(read: suspend () -> String) = runBackup(BackupStatus.IMPORTING) {
        val json = try {
            withContext(io) { read() }
        } catch (e: BackupTooLargeException) {
            return@runBackup SettingsMessage.ImportTooLarge
        } catch (e: IOException) {
            return@runBackup SettingsMessage.IoFailed
        } catch (e: SecurityException) {
            return@runBackup SettingsMessage.IoFailed
        }
        when (val result = importBackup(json)) {
            is Outcome.Ok -> SettingsMessage.Imported(result.value)

            is Outcome.Err -> when (val error = result.error) {
                is ImportError.Invalid -> when (val invalid = error.error) {
                    BackupError.Malformed -> SettingsMessage.ImportMalformed

                    BackupError.TooLarge -> SettingsMessage.ImportTooLarge

                    is BackupError.UnsupportedVersion ->
                        SettingsMessage.ImportUnsupported(invalid.found)
                }

                ImportError.NotReady -> SettingsMessage.ImportNotReady

                ImportError.StagingFailed -> SettingsMessage.ImportStagingFailed

                // 앱 전체가 복구 화면으로 바뀝니다(Task 17).
                ImportError.ApplyFailed -> null
            }
        }
    }

    fun messageShown() {
        local.update { it.copy(message = null) }
    }

    private fun write(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val result = updateSettings(transform)
            if (result is Outcome.Err) {
                val message = when (result.error) {
                    ActionError.NOT_READY -> SettingsMessage.SettingsNotReady
                    else -> SettingsMessage.SettingsStorageFailed
                }
                local.update { it.copy(message = message) }
            }
        }
    }

    /** 진행 중이 아닐 때만 시작합니다. 끝나면 IDLE과 결과 메시지를 함께 반영합니다. */
    private fun runBackup(status: BackupStatus, block: suspend () -> SettingsMessage?) {
        var accepted = false
        local.update {
            accepted = it.backup == BackupStatus.IDLE
            if (accepted) it.copy(backup = status, message = null) else it
        }
        if (!accepted) return
        viewModelScope.launch {
            var message: SettingsMessage? = null
            try {
                message = block()
            } finally {
                local.update { it.copy(backup = BackupStatus.IDLE, message = message) }
            }
        }
    }
}

private const val STOP_TIMEOUT_MS = 5_000L
