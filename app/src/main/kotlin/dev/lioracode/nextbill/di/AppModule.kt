package dev.lioracode.nextbill.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.lioracode.nextbill.BuildConfig
import dev.lioracode.nextbill.core.domain.effect.FailureReporter
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** 설정 화면의 "앱 버전"(Task 23). */
    @Provides
    @Named("appVersion")
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME

    /** 갱신 단계·앱 시작에서 격리한 실패를 기기 로그(`adb logcat -s NextBill`)에 남깁니다. */
    @Provides
    fun provideFailureReporter(): FailureReporter =
        FailureReporter { where, error -> Log.w("NextBill", "$where failed", error) }
}
