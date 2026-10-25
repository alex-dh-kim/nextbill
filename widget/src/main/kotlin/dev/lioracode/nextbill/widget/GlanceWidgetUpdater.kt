package dev.lioracode.nextbill.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.effect.WidgetUpdater
import javax.inject.Inject

class GlanceWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) :
    WidgetUpdater {
    override suspend fun updateAll() {
        NextBillWidget().updateAll(context)
    }
}

interface WidgetPresence {
    suspend fun hasWidgets(): Boolean
}

class GlanceWidgetPresence @Inject constructor(@ApplicationContext private val context: Context) :
    WidgetPresence {
    override suspend fun hasWidgets(): Boolean = GlanceAppWidgetManager(context)
        .getGlanceIds(NextBillWidget::class.java)
        .isNotEmpty()
}
