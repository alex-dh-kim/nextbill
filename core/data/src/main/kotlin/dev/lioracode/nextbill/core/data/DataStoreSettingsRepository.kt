package dev.lioracode.nextbill.core.data

import dev.lioracode.nextbill.core.datastore.SettingsDataSource
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DataStoreSettingsRepository @Inject constructor(private val source: SettingsDataSource) :
    SettingsRepository {
    override val settings: Flow<UserSettings> = source.settings

    override suspend fun update(transform: (UserSettings) -> UserSettings) =
        source.update(transform)

    override suspend fun replace(settings: UserSettings) = source.update { settings }

    override val notificationPermissionRequested: Flow<Boolean> =
        source.notificationPermissionRequested

    override suspend fun markNotificationPermissionRequested() =
        source.markNotificationPermissionRequested()
}
