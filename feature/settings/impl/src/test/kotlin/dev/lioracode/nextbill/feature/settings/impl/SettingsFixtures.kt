package dev.lioracode.nextbill.feature.settings.impl

import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.feature.settings.impl.licenses.LicenseCatalog
import dev.lioracode.nextbill.feature.settings.impl.licenses.parseLicenseCatalog
import java.io.File

/** 시안 05: 기본값(1일 전·오전 9:00·시스템 설정·KRW), 동적 색상 켬, 버전 1.0.0. */
internal val settingsReady = SettingsUiState(
    settings = UserSettings.DEFAULT.copy(useDynamicColor = true),
    appVersion = "1.0.0",
    backup = BackupStatus.IDLE,
    message = null
)

internal fun realLicenseCatalog(): LicenseCatalog = parseLicenseCatalog(
    File(checkNotNull(System.getProperty("nextbill.licensesJson"))).readText()
)
