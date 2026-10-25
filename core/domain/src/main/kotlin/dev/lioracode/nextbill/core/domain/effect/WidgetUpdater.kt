package dev.lioracode.nextbill.core.domain.effect

interface WidgetUpdater {
    suspend fun updateAll()
}
