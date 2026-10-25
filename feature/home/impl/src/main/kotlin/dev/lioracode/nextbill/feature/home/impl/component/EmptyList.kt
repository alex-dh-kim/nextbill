package dev.lioracode.nextbill.feature.home.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbPrimaryButton
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.feature.home.impl.R

/** 시안 01: 영수증 아이콘, 안내 문구, 첫 구독 추가 버튼과 "기기에만 저장" 안내. */
@Composable
internal fun EmptyList(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NextBillTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(NbIcons.ReceiptLong),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    stringResource(R.string.home_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                NbPrimaryButton(
                    text = stringResource(R.string.home_empty_cta),
                    onClick = onAddClick,
                    leadingIcon = NbIcons.Add,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
            }
        }
        NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painterResource(NbIcons.Lock),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.textTertiary
                )
                Text(
                    stringResource(R.string.home_local_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
