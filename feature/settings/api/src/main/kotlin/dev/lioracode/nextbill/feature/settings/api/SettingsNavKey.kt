package dev.lioracode.nextbill.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 설정 탭의 루트. */
@Serializable
data object SettingsNavKey : NavKey

/** 오픈소스 라이선스 화면. */
@Serializable
data object LicensesNavKey : NavKey
