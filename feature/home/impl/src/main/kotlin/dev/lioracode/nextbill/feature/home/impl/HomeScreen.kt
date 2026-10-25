package dev.lioracode.nextbill.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NbShapes
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.feature.home.impl.component.EmptyList
import dev.lioracode.nextbill.feature.home.impl.component.SummaryCard
import dev.lioracode.nextbill.feature.home.impl.component.UpcomingRow

/** 목록 맨 아래 항목이 확장 FAB(56dp) 뒤에 가리지 않도록 FAB 높이 + 위아래 여백 16dp씩을 비웁니다. */
private val FabHeight = 56.dp
private val FabMargin = 16.dp
private val SectionGap = 20.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddClick: () -> Unit,
    onItemClick: (SubscriptionId) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    Column(modifier = modifier.fillMaxSize()) {
        NbTopAppBar(
            title = stringResource(R.string.home_title),
            titleStyle = MaterialTheme.typography.headlineMedium
        )
        when (state) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize())

            is HomeUiState.Ready -> Box(Modifier.fillMaxSize()) {
                val empty = state.upcoming.isEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = NextBillTheme.spacing.margin,
                        end = NextBillTheme.spacing.margin,
                        top = NextBillTheme.spacing.sm,
                        bottom = if (empty) {
                            NextBillTheme.spacing.xl
                        } else {
                            FabHeight + FabMargin * 2
                        }
                    )
                ) {
                    item(key = "summary") {
                        SummaryCard(state, Modifier.padding(bottom = SectionGap))
                    }
                    if (empty) {
                        item(key = "empty") { EmptyList(onAddClick) }
                    } else {
                        item(key = "header") {
                            UpcomingHeader(
                                state.upcoming.size,
                                Modifier.padding(bottom = SectionGap)
                            )
                        }
                        itemsIndexed(state.upcoming, key = { _, item ->
                            item.id.value
                        }) { index, item ->
                            UpcomingRow(
                                item = item,
                                shape = rowShape(index, state.upcoming.lastIndex),
                                isFirst = index == 0,
                                onClick = { onItemClick(item.id) }
                            )
                        }
                    }
                }
                if (!empty) {
                    ExtendedFloatingActionButton(
                        onClick = onAddClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = NextBillTheme.spacing.margin, bottom = FabMargin),
                        shape = NbShapes.Card,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        ),
                        icon = { Icon(painterResource(NbIcons.Add), contentDescription = null) },
                        text = {
                            Text(
                                stringResource(R.string.home_add),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.home_upcoming),
            style = MaterialTheme.typography.titleLarge,
            color = NextBillTheme.colors.textPrimary
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                .padding(horizontal = 6.dp)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

/** 한 카드처럼 보이도록 첫 행은 위쪽, 마지막 행은 아래쪽 모서리만 둥글게 합니다. */
private fun rowShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val radius = 16.dp
    return RoundedCornerShape(
        topStart = if (index == 0) radius else 0.dp,
        topEnd = if (index == 0) radius else 0.dp,
        bottomStart = if (index == lastIndex) radius else 0.dp,
        bottomEnd = if (index == lastIndex) radius else 0.dp
    )
}
