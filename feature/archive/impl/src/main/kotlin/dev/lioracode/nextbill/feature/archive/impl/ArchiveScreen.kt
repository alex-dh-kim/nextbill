package dev.lioracode.nextbill.feature.archive.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbPill
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.format
import dev.lioracode.nextbill.core.ui.LetterAvatar
import dev.lioracode.nextbill.core.ui.cycleText
import dev.lioracode.nextbill.core.ui.monthDayText

/** 화면 콜백. ViewModel 메서드와 화면 이동(Route가 연결)을 한데 묶습니다. */
internal class ArchiveActions(
    val onBack: () -> Unit = {},
    val onOpenDetail: (SubscriptionId) -> Unit = {},
    val onRestore: (SubscriptionId) -> Unit = {},
    val onDelete: (SubscriptionId) -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@Composable
internal fun ArchiveScreen(
    state: ArchiveUiState,
    actions: ArchiveActions,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    val currentActions by rememberUpdatedState(actions)
    val ready = state as? ArchiveUiState.Ready

    val snackbar = remember { SnackbarHostState() }
    val message = ready?.message
    val messageText = message?.let { stringResource(it.textRes()) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            currentActions.onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.surfaceCanvas)) {
        Column(Modifier.fillMaxSize()) {
            NbTopAppBar(
                title = stringResource(R.string.archive_title),
                navigationIcon = NbIcons.ArrowBack,
                navigationContentDescription = stringResource(R.string.archive_back),
                onNavigationClick = actions.onBack
            )
            when {
                ready == null -> Unit
                ready.items.isEmpty() -> EmptyState()
                else -> ArchiveList(ready.items, actions)
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Notice()
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.archive_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = NextBillTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Notice() {
    val colors = NextBillTheme.colors
    NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(NbIcons.Notifications),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(R.string.archive_notice),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun ArchiveList(items: List<Subscription>, actions: ArchiveActions) {
    val colors = NextBillTheme.colors
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Notice() }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.archive_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                NbPill(stringResource(R.string.archive_count, items.size))
            }
        }
        item {
            NbCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    items.forEachIndexed { index, subscription ->
                        if (index > 0) HorizontalDivider(color = colors.borderSubtle)
                        ArchiveRow(
                            subscription = subscription,
                            onOpen = { actions.onOpenDetail(subscription.id) },
                            onRestore = { actions.onRestore(subscription.id) },
                            onDelete = { pendingDelete = subscription.id.value }
                        )
                    }
                }
            }
        }
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.archive_confirm_delete_title)) },
            text = { Text(stringResource(R.string.archive_confirm_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    actions.onDelete(SubscriptionId(id))
                }) {
                    Text(
                        stringResource(R.string.archive_confirm_delete_ok),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.archive_confirm_dismiss))
                }
            }
        )
    }
}

@Composable
private fun ArchiveRow(
    subscription: Subscription,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = NextBillTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    val cancelledAt = subscription.cancelledAt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.archive_row_open),
                role = Role.Button,
                onClick = onOpen
            )
            .heightIn(min = 72.dp)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LetterAvatar(
            subscription.name,
            subscription.colorKey,
            size = 44.dp,
            desaturated = true
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                subscription.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subscription.price.format() + " · " +
                    cycleText(subscription.cycle, subscription.anchorDate),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (cancelledAt != null) {
                Text(
                    stringResource(R.string.archive_row_cancelled_on, monthDayText(cancelledAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1
                )
            }
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painterResource(NbIcons.MoreVert),
                    contentDescription = stringResource(R.string.archive_more, subscription.name),
                    tint = colors.textTertiary
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.archive_menu_restore)) },
                    leadingIcon = {
                        Icon(painterResource(NbIcons.RestoreFromTrash), contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        onRestore()
                    }
                )
                HorizontalDivider(color = colors.borderSubtle)
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.archive_menu_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(NbIcons.Delete),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun ArchiveMessage.textRes(): Int = when (this) {
    ArchiveMessage.RESTORED -> R.string.archive_message_restored
    ArchiveMessage.PERIOD_LIMIT -> R.string.archive_message_period_limit
    ArchiveMessage.NOT_READY -> R.string.archive_message_not_ready
    ArchiveMessage.STORAGE_FAILED -> R.string.archive_message_storage_failed
    ArchiveMessage.MISSING -> R.string.archive_message_missing
}
