package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.model.UserSettings
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: UserSettings = UserSettings.DEFAULT) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    private val requested = MutableStateFlow(false)

    override val settings: Flow<UserSettings> = state
    override val notificationPermissionRequested: Flow<Boolean> = requested

    private val replaceFailure = CountedFailure()

    /** 다음 [failNextReplaceTimes]번의 [replace]를 이 오류로 실패시킵니다. */
    var failNextReplace: Throwable?
        get() = replaceFailure.error
        set(value) {
            replaceFailure.error = value
        }

    var failNextReplaceTimes: Int
        get() = replaceFailure.times
        set(value) {
            replaceFailure.times = value
        }

    /** 설정하면 다음 [update] 한 번을 이 오류로 실패시킵니다. */
    @Volatile var failNextUpdate: Throwable? = null

    private val replaceCounter = AtomicInteger()
    val replaceCount: Int get() = replaceCounter.get()

    /** 현재 값(테스트 확인용). */
    val current: UserSettings get() = state.value

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        failNextUpdate?.let {
            failNextUpdate = null
            throw it
        }
        state.update { transform(it) }
    }

    override suspend fun replace(settings: UserSettings) {
        replaceFailure.throwIfArmed()
        state.value = settings
        replaceCounter.incrementAndGet()
    }

    override suspend fun markNotificationPermissionRequested() {
        requested.value = true
    }
}
