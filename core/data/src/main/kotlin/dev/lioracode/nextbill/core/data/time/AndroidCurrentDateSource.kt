package dev.lioracode.nextbill.core.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 오늘 날짜(ADR-12). 날짜·시각·시간대 변경 브로드캐스트, 프로세스 ON_START, 앱이 떠 있는 동안의
 * 다음 현지 자정에 다시 계산합니다. 코루틴 delay는 깊은 잠(doze/deep sleep) 중 늦게 깨어날 수 있으나,
 * 그 경우도 DATE_CHANGED 브로드캐스트와 ON_START가 보완합니다.
 */
@Singleton
class AndroidCurrentDateSource @Inject constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
    @ApplicationScope scope: CoroutineScope
) : CurrentDateSource {
    private val state = MutableStateFlow(LocalDate.now(clock))
    override val today: StateFlow<LocalDate> = state

    /** 자정 대기를 깨워 남은 시간을 다시 계산하게 합니다. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refresh()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        scope.launch(Dispatchers.Main.immediate) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) = refresh()
                }
            )
        }
        scope.launch {
            while (true) {
                val now = ZonedDateTime.now(clock)
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                val millis = Duration.between(now.toInstant(), midnight.toInstant()).toMillis()
                withTimeoutOrNull(millis.coerceAtLeast(1)) { wake.receive() }
                state.value = LocalDate.now(clock)
            }
        }
    }

    private fun refresh() {
        state.value = LocalDate.now(clock)
        wake.trySend(Unit)
    }
}
