package dev.lioracode.nextbill.core.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 앱 시작 상태(설계 문서 ADR-7). */
sealed interface StartupState {
    data object Recovering : StartupState

    data object Ready : StartupState

    data object RecoveryFailed : StartupState
}

/** 쓰기 관문의 판정. */
sealed interface Admission<out T> {
    data class Ran<T>(val value: T) : Admission<T>

    /** 준비 상태가 아니어서 실행하지 않았습니다. */
    data object NotReady : Admission<Nothing>

    /** 화면을 연 뒤 데이터 세대가 바뀌어(가져오기) 실행하지 않았습니다. */
    data object Stale : Admission<Nothing>
}

/** [DataCoordinator.recovery] 블록 안에서만 쓰는 상태 변경. */
internal interface RecoveryScope {
    fun setState(s: StartupState)

    fun setGeneration(revision: Long)
}

/**
 * 영속 데이터 쓰기의 관문(설계 문서 ADR-7, ADR-13). Mutex 하나로 저장·설정·알림 처리·지난 알림 정리·
 * 가져오기·복구를 한 번에 하나씩 실행하고, 잠금을 얻은 **뒤에** 준비 상태와 데이터 세대를 검사합니다.
 *
 * - 재진입하지 않습니다: 블록 안에서 [write]·[writeWhenReady]·[recovery]를 다시 부르지 않습니다.
 * - 잠금 순서는 DerivedStateRefresher의 runLock → 이 Mutex → SyncRemindersUseCase의 alarmLock 하나뿐입니다.
 *   그래서 write·writeWhenReady·recovery 블록 안에서 `refreshNow()`를 부르지 않습니다(`request()`는 잠금 밖에서).
 *   alarmLock 안에서는 suspend하지 않고 이 Mutex를 잡지 않습니다.
 * - 알림 표시를 바꾸는 `ReminderPoster.show`·`pruneExpired`는 이 잠금 안에서만 부릅니다.
 */
@Singleton
class DataCoordinator @Inject constructor() {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<StartupState>(StartupState.Recovering)
    private val mutableGeneration = MutableStateFlow(0L)

    /** 처음 값은 Recovering. [recovery] 블록 안에서만 바뀝니다. */
    val state: StateFlow<StartupState> = mutableState.asStateFlow()

    /** 영속 데이터 세대(`repo.revision()`)의 메모리 사본. */
    val generation: StateFlow<Long> = mutableGeneration.asStateFlow()

    private val scope = object : RecoveryScope {
        override fun setState(s: StartupState) {
            mutableState.value = s
        }

        override fun setGeneration(revision: Long) {
            mutableGeneration.value = revision
        }
    }

    /**
     * 잠금을 얻은 뒤: Ready가 아니면 [Admission.NotReady], [expectedGeneration]이 지금 세대와 다르면
     * [Admission.Stale], 아니면 [block]을 실행합니다.
     */
    suspend fun <T> write(expectedGeneration: Long? = null, block: suspend () -> T): Admission<T> =
        mutex.withLock {
            when {
                mutableState.value != StartupState.Ready -> Admission.NotReady

                expectedGeneration != null && expectedGeneration != mutableGeneration.value ->
                    Admission.Stale

                else -> Admission.Ran(block())
            }
        }

    /**
     * 수신기용. Recovering이면 잠금 밖에서 끝날 때까지 기다린 뒤 잠금을 잡고 검사합니다. 잠금을 얻었는데
     * 다시 Recovering이면(retryRecovery의 두 블록 사이) 잠금을 놓고 다시 기다립니다. RecoveryFailed면
     * **잠금을 쥔 채** [onNotReady]를 실행하고 [Admission.NotReady]를 돌려줍니다.
     */
    suspend fun <T> writeWhenReady(
        onNotReady: suspend () -> Unit = {},
        block: suspend () -> T
    ): Admission<T> {
        while (true) {
            state.first { it != StartupState.Recovering }
            val admission: Admission<T>? = mutex.withLock {
                when (mutableState.value) {
                    StartupState.Recovering -> null

                    StartupState.Ready -> Admission.Ran(block())

                    StartupState.RecoveryFailed -> {
                        onNotReady()
                        Admission.NotReady
                    }
                }
            }
            if (admission != null) return admission
        }
    }

    /** AppStartup·ImportBackupUseCase 전용. 같은 Mutex, 검사 없음. */
    internal suspend fun <T> recovery(block: suspend RecoveryScope.() -> T): T =
        mutex.withLock { scope.block() }
}
