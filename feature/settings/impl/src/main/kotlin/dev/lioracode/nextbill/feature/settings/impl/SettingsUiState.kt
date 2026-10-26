package dev.lioracode.nextbill.feature.settings.impl

import dev.lioracode.nextbill.core.model.UserSettings

data class SettingsUiState(
    val settings: UserSettings,
    val appVersion: String,
    val backup: BackupStatus,
    val message: SettingsMessage?
)

/** 진행 중(EXPORTING·IMPORTING)에는 내보내기·가져오기 두 버튼이 모두 비활성입니다. */
enum class BackupStatus { IDLE, EXPORTING, IMPORTING }

/**
 * 스낵바로 한 번 보여 준 뒤 `messageShown()`으로 지웁니다. 가져오기 적용 실패(ApplyFailed)는 메시지 대신
 * 앱 전체가 복구 화면으로 바뀌므로 여기에 없습니다.
 */
sealed interface SettingsMessage {
    data object Exported : SettingsMessage

    data class Imported(val count: Int) : SettingsMessage

    data object ImportMalformed : SettingsMessage

    data object ImportTooLarge : SettingsMessage

    data class ImportUnsupported(val version: Int) : SettingsMessage

    data object ImportNotReady : SettingsMessage

    data object ImportStagingFailed : SettingsMessage

    /** 파일을 열거나 읽거나 쓰지 못했습니다. */
    data object IoFailed : SettingsMessage

    data object ExportNotReady : SettingsMessage

    data object SettingsNotReady : SettingsMessage

    /** 설정 저장 실패. 내보낼 데이터를 읽지 못한 경우도 같은 문구입니다. */
    data object SettingsStorageFailed : SettingsMessage
}
