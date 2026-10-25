package dev.lioracode.nextbill.core.domain.repository

import dev.lioracode.nextbill.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun update(transform: (UserSettings) -> UserSettings)

    suspend fun replace(settings: UserSettings)

    val notificationPermissionRequested: Flow<Boolean>

    suspend fun markNotificationPermissionRequested()
}
