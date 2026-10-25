package dev.lioracode.nextbill.core.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.core.domain.effect.ReminderAlarm
import dev.lioracode.nextbill.core.domain.effect.ReminderPoster
import dev.lioracode.nextbill.core.notifications.AlarmReminderAlarm
import dev.lioracode.nextbill.core.notifications.AndroidInexactAlarms
import dev.lioracode.nextbill.core.notifications.InexactAlarms
import dev.lioracode.nextbill.core.notifications.ReminderNotifier

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    abstract fun bindInexactAlarms(impl: AndroidInexactAlarms): InexactAlarms

    @Binds
    abstract fun bindReminderAlarm(impl: AlarmReminderAlarm): ReminderAlarm

    @Binds
    abstract fun bindReminderPoster(impl: ReminderNotifier): ReminderPoster
}
