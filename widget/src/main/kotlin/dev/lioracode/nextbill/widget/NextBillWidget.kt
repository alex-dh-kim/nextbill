package dev.lioracode.nextbill.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import dev.lioracode.nextbill.core.model.CurrencyCode
import dev.lioracode.nextbill.core.model.Subscription
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 위젯과 수신기는 Hilt 주입 대상이 아니라서 진입점으로 의존성을 얻습니다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun subscriptions(): SubscriptionRepository

    fun settings(): SettingsRepository

    fun dateSource(): CurrentDateSource

    fun dayChange(): DayChangeScheduler

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/**
 * 홈 화면 위젯(시안 06). 처음 값은 [provideGlance]에서 계산해 두고(세션이 이미 죽었어도 다시 그려집니다),
 * 살아 있는 세션은 날짜·데이터·설정 흐름을 구독해 바뀔 때 스스로 다시 그립니다.
 * 자정 알람·저장 직후 [updateAll]은 세션이 없을 때를 위한 보완입니다.
 */
class NextBillWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = context.widgetEntryPoint()
        val initial = widgetState(
            subscriptions = entry.subscriptions().getAll(),
            today = entry.dateSource().today.value,
            defaultCurrency = entry.settings().settings.first().defaultCurrency
        )
        val states = widgetStates(
            today = entry.dateSource().today,
            subscriptions = entry.subscriptions().observeAll(),
            defaultCurrency = entry.settings().settings.map { it.defaultCurrency }
        )
        provideContent {
            val state by states.collectAsState(initial)
            WidgetContent(state, LocalSize.current)
        }
    }
}

/** 날짜, 구독 목록, 기본 통화 중 하나라도 바뀌면 새 [WidgetState]를 냅니다. */
internal fun widgetStates(
    today: Flow<LocalDate>,
    subscriptions: Flow<List<Subscription>>,
    defaultCurrency: Flow<CurrencyCode>
): Flow<WidgetState> = combine(today, subscriptions, defaultCurrency) { date, subs, currency ->
    widgetState(subs, date, currency)
}
