package com.exodidio.tune.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.library.LocalLibraryScanner
import com.exodidio.tune.library.ScanPermission
import com.exodidio.tune.library.audioPermission
import com.exodidio.tune.sync.AndroidSyncRuntime
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListDividerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlinx.coroutines.launch

@Composable
internal fun MusicSyncContent(
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember(context.applicationContext) {
        LocalLibraryScanner(context.applicationContext, AndroidSyncRuntime.syncStore())
    }
    val scanState by scanner.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? Activity
        val rationale = activity?.shouldShowRequestPermissionRationale(audioPermission()) == true
        scope.launch {
            scanner.refreshPermission(granted, rationale)
            if (granted) scanner.scan()
        }
    }
    LaunchedEffect(scanner) {
        val granted = context.checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED
        val activity = context as? Activity
        val rationale = !granted && activity?.shouldShowRequestPermissionRationale(audioPermission()) == true
        scanner.refreshPermission(granted, rationale)
    }
    fun startScan() {
        if (context.checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED) {
            scope.launch { scanner.refreshPermission(true, false); scanner.scan() }
        } else {
            permissionLauncher.launch(audioPermission())
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActionList(
            items = listOf(
                ActionListItem(
                    labelRes = R.string.library_scan_local,
                    leadingSymbol = MaterialSymbols.Refresh,
                    leadingIconTint = colors.primary,
                    trailingContent = if (scanState.scanning) {
                        { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                    } else {
                        null
                    },
                    onClick = ::startScan,
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
            dividerStyle = ActionListDividerStyle.FullWidth,
        )
        when (scanState.permission) {
            ScanPermission.NeedsRationale -> Text(
                text = stringResource(R.string.music_sync_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
            ScanPermission.Denied -> Text(
                text = stringResource(R.string.music_sync_permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
            else -> Unit
        }
        scanState.lastResult?.let { result ->
            Text(
                text = stringResource(R.string.music_sync_last_result, result.inserted, result.skipped),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
        scanState.error?.let { error ->
            Text(
                text = stringResource(R.string.music_sync_error, error),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
    }
}
