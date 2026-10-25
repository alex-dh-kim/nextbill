package dev.lioracode.nextbill

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.notifications.ensureChannels
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class NextBillApplication : Application() {
    @Inject lateinit var startup: AppStartup

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
        // 복구 → Ready → 첫 sync가 알람을 다시 겁니다(설계 문서 ADR-4·7). start()는 취소 외 예외를 던지지 않습니다.
        scope.launch { startup.start() }
    }
}
