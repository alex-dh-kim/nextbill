package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.effect.DayChangeScheduler
import dev.lioracode.nextbill.core.domain.effect.ReminderPoster
import dev.lioracode.nextbill.core.domain.effect.WidgetUpdater
import dev.lioracode.nextbill.core.domain.time.CurrentDateSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 저장과 분리된 파생 상태 갱신(설계 문서 ADR-13): 알림 sync·지난 알림 정리 → 자정 예약 → 위젯.
 * 각 단계의 실패는 따로 격리하고, 코루틴 취소는 삼키지 않습니다.
 */
@Singleton
class DerivedStateRefresher @Inject constructor(
    private val coordinator: DataCoordinator,
    private val sync: SyncRemindersUseCase,
    private val poster: ReminderPoster,
    private val widget: WidgetUpdater,
    private val dayChange: DayChangeScheduler,
    private val dateSource: CurrentDateSource,
    @ApplicationScope scope: CoroutineScope
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    /** [request]의 실행과 [refreshNow]를 한 번에 하나씩 실행합니다. */
    private val runLock = Mutex()

    init {
        // 단계가 던진 취소 예외(예: 구현 안의 시간 초과)는 step이 실패로 처리하므로, 이 루프는 scope가
        // 취소될 때만 끝납니다.
        scope.launch {
            for (unit in requests) refreshNow()
        }
    }

    /** 즉시 반환합니다. 실행 중에 온 요청은 하나로 합쳐 끝난 뒤 최신 데이터로 한 번 더 실행합니다. */
    fun request() {
        requests.trySend(Unit)
    }

    /** 앱 시작·수신기용. 한 번 실행하고 끝날 때까지 기다립니다. */
    suspend fun refreshNow() {
        runLock.withLock {
            // NotReady면 건너뜁니다. 정리도 잠금 안이라 알림 처리의 읽기·합치기·게시와 겹치지 않습니다.
            coordinator.write {
                step { sync() }
                // 잠금을 기다리는 사이 날짜가 바뀔 수 있으므로 잠금 안에서 읽습니다.
                val today = dateSource.today.value
                step { poster.pruneExpired(today) }
            }
            step { dayChange.scheduleNext() }
            step { widget.updateAll() }
        }
    }

    /**
     * 한 단계의 실패를 격리합니다. 이 코루틴 자체가 취소됐으면 취소를 그대로 전파하고, 단계 안에서 생긴
     * 취소 예외(예: TimeoutCancellationException)는 그 단계의 실패로 보고 다음 단계로 넘어갑니다.
     */
    private suspend inline fun step(block: () -> Unit) {
        try {
            runCatchingNonCancellation(block)
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
        }
    }
}
