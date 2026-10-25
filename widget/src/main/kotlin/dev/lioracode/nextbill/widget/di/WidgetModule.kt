package dev.lioracode.nextbill.widget.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.domain.effect.WidgetUpdater
import dev.lioracode.nextbill.widget.GlanceWidgetPresence
import dev.lioracode.nextbill.widget.GlanceWidgetUpdater
import dev.lioracode.nextbill.widget.MidnightRefresh
import dev.lioracode.nextbill.widget.WidgetPresence

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater

    @Binds
    abstract fun bindDayChangeScheduler(impl: MidnightRefresh): DayChangeScheduler

    @Binds
    abstract fun bindWidgetPresence(impl: GlanceWidgetPresence): WidgetPresence
}
