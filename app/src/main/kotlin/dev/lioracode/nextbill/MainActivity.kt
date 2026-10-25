package dev.lioracode.nextbill

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 설정을 읽고 시작 복구가 끝날 때까지(Ready 또는 RecoveryFailed) 시작 화면을 유지합니다.
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val dataGeneration by viewModel.dataGeneration.collectAsStateWithLifecycle()
            val ready = uiState as? MainUiState.Ready
            val darkTheme = when (ready?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }

            // 시스템 바 아이콘 색을 시스템 설정이 아니라 앱 안에서 정한 테마에 맞춥니다(NiA와 같은 방식).
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { darkTheme }
                )
                onDispose {}
            }

            NextBillTheme(darkTheme = darkTheme, dynamicColor = ready?.useDynamicColor == true) {
                MainContent(
                    uiState = uiState,
                    dataGeneration = dataGeneration,
                    onRetry = viewModel::retryRecovery,
                    onDiscard = viewModel::discardPendingImport
                )
            }
        }
    }
}

/** androidx.activity의 기본 3버튼 내비게이션 scrim과 같은 값입니다(NiA와 같음). */
private val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
private fun MainContent(
    uiState: MainUiState,
    dataGeneration: Long,
    onRetry: () -> Unit,
    onDiscard: () -> Unit
) {
    when (uiState) {
        is MainUiState.Ready -> NextBillApp(dataGeneration)

        MainUiState.RecoveryFailed -> RecoveryScreen(onRetry = onRetry, onDiscard = onDiscard)

        // 처음에는 시작 화면이 가립니다. "다시 시도" 중에는 진행 표시를 보여 줍니다.
        MainUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(NextBillTheme.colors.surfaceCanvas),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
