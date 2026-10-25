package dev.lioracode.nextbill.core.domain

import javax.inject.Qualifier

@Qualifier
annotation class DefaultDispatcher

@Qualifier
annotation class IoDispatcher

@Qualifier
annotation class ApplicationScope
