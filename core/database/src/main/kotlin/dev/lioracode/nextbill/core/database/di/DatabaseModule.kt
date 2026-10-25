package dev.lioracode.nextbill.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.core.database.NextBillDatabase
import dev.lioracode.nextbill.core.database.ScheduledReminderDao
import dev.lioracode.nextbill.core.database.SubscriptionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NextBillDatabase =
        Room.databaseBuilder(
            context,
            NextBillDatabase::class.java,
            NextBillDatabase.FILE_NAME
        ).build()

    @Provides
    fun provideSubscriptionDao(db: NextBillDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideScheduledReminderDao(db: NextBillDatabase): ScheduledReminderDao =
        db.scheduledReminderDao()
}
