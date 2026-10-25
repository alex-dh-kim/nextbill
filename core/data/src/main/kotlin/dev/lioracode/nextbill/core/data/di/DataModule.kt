package dev.lioracode.nextbill.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.core.data.DataStoreSettingsRepository
import dev.lioracode.nextbill.core.data.OfflineSubscriptionRepository
import dev.lioracode.nextbill.core.data.RoomScheduledReminderRepository
import dev.lioracode.nextbill.core.data.backup.FileImportStaging
import dev.lioracode.nextbill.core.data.backup.JsonBackupCodec
import dev.lioracode.nextbill.core.data.time.AndroidCurrentDateSource
import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.ImportStaging
import dev.lioracode.nextbill.core.domain.repository.ScheduledReminderRepository
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    fun subscriptions(impl: OfflineSubscriptionRepository): SubscriptionRepository

    @Binds
    fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun reminders(impl: RoomScheduledReminderRepository): ScheduledReminderRepository

    @Binds
    fun importStaging(impl: FileImportStaging): ImportStaging

    @Binds
    fun backupCodec(impl: JsonBackupCodec): BackupCodec

    @Binds
    fun currentDate(impl: AndroidCurrentDateSource): CurrentDateSource
}
