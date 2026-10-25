package dev.lioracode.nextbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.lioracode.nextbill.core.designsystem.component.NbDestructiveTextButton
import dev.lioracode.nextbill.core.designsystem.component.NbPrimaryButton
import dev.lioracode.nextbill.core.designsystem.component.NbSecondaryButton
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme

/**
 * 남은 가져오기를 다시 시도해도 적용하지 못했을 때 탭 대신 보여 주는 전체 화면(설계 문서 ADR-7).
 * "가져오기 취소"는 일부만 바뀐 데이터가 남을 수 있어 확인을 한 번 더 받습니다.
 */
@Composable
internal fun RecoveryScreen(
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val spacing = NextBillTheme.spacing

    Surface(modifier = modifier.fillMaxSize(), color = NextBillTheme.colors.surfaceCanvas) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(spacing.margin),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.titleLarge,
                color = NextBillTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(spacing.xl))
            NbPrimaryButton(
                text = stringResource(R.string.recovery_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacing.md))
            NbSecondaryButton(
                text = stringResource(R.string.recovery_discard),
                onClick = { confirmDiscard = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.recovery_discard_confirm_title)) },
            text = { Text(stringResource(R.string.recovery_discard_confirm_body)) },
            confirmButton = {
                NbDestructiveTextButton(
                    text = stringResource(R.string.recovery_discard),
                    onClick = {
                        confirmDiscard = false
                        onDiscard()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.recovery_discard_dismiss))
                }
            }
        )
    }
}
