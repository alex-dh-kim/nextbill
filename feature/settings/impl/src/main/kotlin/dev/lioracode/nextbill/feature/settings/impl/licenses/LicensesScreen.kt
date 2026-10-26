package dev.lioracode.nextbill.feature.settings.impl.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lioracode.nextbill.core.designsystem.component.NbCard
import dev.lioracode.nextbill.core.designsystem.component.NbTopAppBar
import dev.lioracode.nextbill.core.designsystem.theme.NbIcons
import dev.lioracode.nextbill.core.designsystem.theme.NextBillTheme
import dev.lioracode.nextbill.feature.settings.impl.R

/** 읽는 동안은 목록이 비어 있습니다. */
@Composable
internal fun LicensesRoute(onBack: () -> Unit, viewModel: LicensesViewModel = hiltViewModel()) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    LicensesScreen(catalog = catalog, onBack = onBack)
}

@Composable
internal fun LicensesScreen(
    catalog: LicenseCatalog?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NextBillTheme.colors
    var openIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(modifier = modifier.fillMaxSize().background(colors.surfaceCanvas)) {
        NbTopAppBar(
            title = stringResource(R.string.licenses_title),
            navigationIcon = NbIcons.ArrowBack,
            navigationContentDescription = stringResource(R.string.licenses_back),
            onNavigationClick = onBack
        )
        if (catalog != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = NextBillTheme.spacing.margin,
                    vertical = 8.dp
                )
            ) {
                item {
                    NbCard(modifier = Modifier.fillMaxWidth()) {
                        catalog.entries.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(color = colors.borderSubtle)
                            LicenseRow(entry, onClick = { openIndex = index })
                        }
                    }
                }
            }
            openIndex?.let { catalog.entries.getOrNull(it) }?.let { entry ->
                LicenseDialog(
                    entry = entry,
                    text = catalog.texts[entry.license].orEmpty(),
                    onDismiss = { openIndex = null }
                )
            }
        }
    }
}

@Composable
private fun LicenseRow(entry: LicenseEntry, onClick: () -> Unit) {
    val colors = NextBillTheme.colors
    val artifacts = entry.artifact.lines()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(entry.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Text(
            if (artifacts.size == 1) {
                artifacts.single()
            } else {
                pluralStringResource(
                    R.plurals.licenses_artifact_count,
                    artifacts.size,
                    artifacts.size
                )
            },
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
        Text(
            entry.license,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary
        )
    }
}

@Composable
private fun LicenseDialog(entry: LicenseEntry, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(entry.url, style = MaterialTheme.typography.labelMedium)
                Text(entry.artifact, style = MaterialTheme.typography.labelSmall)
                entry.notice?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.licenses_close)) }
        }
    )
}
