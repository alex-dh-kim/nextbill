package dev.lioracode.nextbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lioracode.nextbill.core.domain.ApplicationScope
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.usecase.AppStartup
import dev.lioracode.nextbill.core.domain.usecase.StartupState
import dev.lioracode.nextbill.core.model.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainUiState {
    /** 설정을 아직 못 읽었거나 시작 복구 중입니다. 시작 화면(SplashScreen)을 유지합니다. */
    data object Loading : MainUiState

    data class Ready(val themeMode: ThemeMode, val useDynamicColor: Boolean) : MainUiState

    /** 남은 가져오기를 적용하지 못했습니다. 탭 대신 복구 화면만 보여 줍니다(설계 문서 ADR-7). */
    data object RecoveryFailed : MainUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settings: SettingsRepository,
    private val startup: AppStartup,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {
    /** 시작 화면 유지 조건을 Activity가 바로 읽으므로 구독자 유무와 관계없이 계속 갱신합니다. */
    val uiState: StateFlow<MainUiState> = combine(settings.settings, startup.state) { s, state ->
        when (state) {
            StartupState.Recovering -> MainUiState.Loading
            StartupState.Ready -> MainUiState.Ready(s.themeMode, s.useDynamicColor)
            StartupState.RecoveryFailed -> MainUiState.RecoveryFailed
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    /** 바뀌면 앱 셸이 모든 탭 스택을 루트로 되돌립니다. */
    val dataGeneration: StateFlow<Long> = startup.dataGeneration

    // 복구 전이는 화면 수명과 관계없이 끝까지 실행합니다. viewModelScope에서 중간에 취소되면
    // Recovering에 머물러 다음 실행까지 시작 화면이 풀리지 않을 수 있습니다.
    fun retryRecovery() {
        appScope.launch { startup.retryRecovery() }
    }

    fun discardPendingImport() {
        appScope.launch { startup.discardPendingImport() }
    }
}
