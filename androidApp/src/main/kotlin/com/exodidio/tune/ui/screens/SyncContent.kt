package com.exodidio.tune.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import com.exodidio.tune.R
import com.exodidio.tune.SyncUiState
import com.exodidio.tune.pairing.PairingFailure
import com.exodidio.tune.sync.AndroidSyncState
import com.exodidio.tune.ui.components.TuneDialog
import com.exodidio.tune.ui.components.TunePillButton
import com.exodidio.tune.ui.components.TunePillButtonVariant
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.Card
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.theme.LocalTuneColors

private const val MobileSyncHelpUrl = "https://tune.pages.dev/faq/mobile-sync"

@Composable
internal fun SyncContent(
    syncUiState: SyncUiState,
    onUnpair: () -> Unit,
    onOpenExternalUrl: (String) -> Unit = {},
    onScreenVisible: () -> Unit = {},
    onScreenHidden: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onScreenVisible()
        onDispose(onScreenHidden)
    }
    val colors = LocalTuneColors.current
    val isSyncRunning = syncUiState.librarySync is AndroidSyncState.Running
    var showRevokeConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            syncUiState.desktop != null -> {
                HeroCard(
                    symbol = MaterialSymbols.DesktopWindows,
                    title = syncUiState.desktop.displayName,
                    description = stringResource(
                        if (syncUiState.isMqttConnected) R.string.sync_paired_device_description
                        else R.string.sync_ready_to_connect_description,
                    ),
                    belowTitle = {
                        MqttConnectionBadge(
                            isConnected = syncUiState.isMqttConnected,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    },
                    bottomContent = {
                        TunePillButton(
                            label = stringResource(R.string.sync_revoke),
                            onClick = { showRevokeConfirmation = true },
                            variant = TunePillButtonVariant.Destructive,
                            enabled = !isSyncRunning,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                        )
                    },
                )
            }
            syncUiState.isPairing -> HeroCard(
                symbol = MaterialSymbols.Sync,
                title = stringResource(R.string.sync_waiting_title),
                description = stringResource(R.string.sync_waiting_description),
            )
            else -> HeroCard(
                symbol = MaterialSymbols.Power,
                title = stringResource(R.string.sync_empty_title),
                description = stringResource(R.string.sync_empty_description),
            )
        }
        syncUiState.failure?.let { failure ->
            Text(
                text = stringResource(failure.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
        ActionList(
            items = listOf(
                ActionListItem(
                    labelRes = R.string.sync_help,
                    leadingSymbol = MaterialSymbols.Info,
                    onClick = { onOpenExternalUrl(MobileSyncHelpUrl) },
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
        )
        SyncProgressCard(
            syncState = syncUiState.librarySync,
            lastSyncedAtMillis = syncUiState.lastSyncedAtMillis,
        )
        if (syncUiState.libraryAnalysisEnabled) LibraryAnalysisCard(syncUiState.libraryAnalysis)
    }
    if (showRevokeConfirmation && !isSyncRunning) {
        TuneDialog(
            title = stringResource(R.string.sync_revoke_confirm_title),
            description = stringResource(R.string.sync_revoke_confirm_description),
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { showRevokeConfirmation = false },
            confirmLabel = stringResource(R.string.sync_revoke),
            onConfirm = { showRevokeConfirmation = false; onUnpair() },
            confirmVariant = TunePillButtonVariant.Destructive,
        )
    }
}

@Composable
private fun LibraryAnalysisCard(progress: com.exodidio.tune.sync.LibraryAnalysisProgress) {
    if (progress.totalTracks == 0) return
    Card(contentPadding = PaddingValues(20.dp)) {
        Text(stringResource(R.string.sync_library_analysis), style = MaterialTheme.typography.labelMedium, color = LocalTuneColors.current.textMuted)
        Text(
            stringResource(R.string.insight_percent_value, progress.analyzedTracks * 100 / progress.totalTracks),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = LocalTuneColors.current.textMain,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MqttConnectionBadge(isConnected: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalTuneColors.current
    val label = stringResource(if (isConnected) R.string.sync_status_online else R.string.sync_status_offline)
    val statusColor = if (isConnected) colors.success else colors.primary
    Row(
        modifier = modifier
            .background(colors.buttonSecondary, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
        )
    }
}

private fun PairingFailure.messageRes(): Int = when (this) {
    PairingFailure.AlreadyPaired -> R.string.sync_error_already_paired
    is PairingFailure.InvalidQr -> R.string.sync_error_invalid_qr
    is PairingFailure.Transport -> R.string.sync_error_transport
    PairingFailure.TimedOut -> R.string.sync_error_timeout
    PairingFailure.Rejected -> R.string.sync_error_rejected
    PairingFailure.Expired -> R.string.sync_error_expired
    PairingFailure.InvalidResponse -> R.string.sync_error_invalid_response
}

@Composable
private fun SyncProgressCard(
    syncState: AndroidSyncState,
    lastSyncedAtMillis: Long?,
    modifier: Modifier = Modifier,
) {
    if (syncState is AndroidSyncState.Idle) return

    val colors = LocalTuneColors.current

    val statusText = when (syncState) {
        is AndroidSyncState.Running -> {
            if (syncState.total == 0) {
                stringResource(R.string.sync_progress_connecting)
            } else {
                stringResource(R.string.sync_progress_syncing)
            }
        }
        is AndroidSyncState.Completed -> stringResource(R.string.sync_notification_complete)
        is AndroidSyncState.Failed -> stringResource(R.string.sync_notification_failed)
        AndroidSyncState.Idle -> ""
    }

    val progressFloat: Float? = when (syncState) {
        is AndroidSyncState.Running -> {
            if (syncState.total == 0) null
            else (syncState.completed.toFloat() / syncState.total.toFloat()).coerceIn(0f, 1f)
        }
        is AndroidSyncState.Completed -> 1.0f
        is AndroidSyncState.Failed -> 0f
        AndroidSyncState.Idle -> null
    }

    val percentageText = progressFloat?.let { "${(it * 100).toInt()}%" }

    Card(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
    ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    percentageText?.let { pct ->
                        Text(
                            text = pct,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                TuneProgressBar(
                    progress = progressFloat,
                    color = colors.textMain,
                    trackColor = colors.buttonSecondary,
                )
                Text(
                    text = lastSyncedAtMillis?.let { timestamp ->
                        stringResource(
                            R.string.sync_last_synced,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp)),
                        )
                    } ?: stringResource(R.string.sync_never_synced),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
    }
}

@Composable
private fun TuneProgressBar(
    progress: Float?,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
