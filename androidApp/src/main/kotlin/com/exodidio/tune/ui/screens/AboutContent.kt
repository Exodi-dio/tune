package com.exodidio.tune.ui.screens

import android.app.DownloadManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.BuildConfig
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListDividerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.Card
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.TunePillButton
import com.exodidio.tune.ui.components.TunePillButtonVariant
import com.exodidio.tune.ui.theme.LocalTuneColors
import com.exodidio.tune.update.AppRelease
import com.exodidio.tune.update.enqueueUpdateDownload
import com.exodidio.tune.update.fetchLatestRelease
import com.exodidio.tune.update.installUpdateApk
import com.exodidio.tune.update.isUpdateAvailable
import com.exodidio.tune.update.updateApkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TuneGithubUrl = "https://github.com/Exodi-dio/tune"
private const val TuneLicenseUrl = "https://github.com/Exodi-dio/tune/blob/master/LICENSE"

private sealed interface AboutUpdateState {
    data object Idle : AboutUpdateState
    data object Checking : AboutUpdateState
    data class UpToDate(val latestVersion: String) : AboutUpdateState
    data class Available(val release: AppRelease) : AboutUpdateState
    data class NotesOnly(val release: AppRelease) : AboutUpdateState
    data object Error : AboutUpdateState
}

@Composable
internal fun AboutContent(
    onOpenExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<AboutUpdateState>(AboutUpdateState.Idle) }
    var downloading by remember { mutableStateOf(false) }

    fun checkForUpdates() {
        if (updateState == AboutUpdateState.Checking) return
        scope.launch {
            updateState = AboutUpdateState.Checking
            updateState = try {
                val release = fetchLatestRelease()
                val current = BuildConfig.VERSION_NAME
                if (!isUpdateAvailable(current, release.tag)) {
                    AboutUpdateState.UpToDate(release.version)
                } else if (release.apkUrl.isNullOrBlank()) {
                    AboutUpdateState.NotesOnly(release)
                } else {
                    AboutUpdateState.Available(release)
                }
            } catch (_: Exception) {
                AboutUpdateState.Error
            }
        }
    }

    fun downloadAndInstall(release: AppRelease) {
        val url = release.apkUrl ?: return
        if (downloading) return
        scope.launch {
            downloading = true
            try {
                val downloadId = withContext(Dispatchers.IO) {
                    enqueueUpdateDownload(context.applicationContext, url, release.tag)
                }
                val apkFile = updateApkFile(context.applicationContext, release.tag)
                val installed = withContext(Dispatchers.IO) {
                    awaitDownloadSuccess(context.applicationContext, downloadId)
                }
                if (installed) {
                    try {
                        installUpdateApk(context.applicationContext, apkFile)
                    } catch (_: Exception) {
                        updateState = AboutUpdateState.Error
                    }
                } else {
                    updateState = AboutUpdateState.Error
                }
            } catch (_: Exception) {
                updateState = AboutUpdateState.Error
            } finally {
                downloading = false
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(
            title = stringResource(R.string.app_name),
            description = stringResource(R.string.about_description),
        ) {
            Image(
                painter = painterResource(R.drawable.tune_about_app_icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        }
        ActionList(
            items = listOf(
                ActionListItem(
                    labelRes = R.string.about_version,
                    trailingContent = {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textMuted,
                        )
                    },
                ),
                ActionListItem(
                    labelRes = R.string.about_update,
                    trailingContent = {
                        when (updateState) {
                            AboutUpdateState.Checking -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            AboutUpdateState.Idle -> Text(
                                text = stringResource(R.string.about_update_check),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                            is AboutUpdateState.UpToDate -> Text(
                                text = stringResource(R.string.about_update_up_to_date),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                            is AboutUpdateState.Available -> Text(
                                text = stringResource(R.string.about_update_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                            is AboutUpdateState.NotesOnly -> Text(
                                text = stringResource(R.string.about_update_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                            AboutUpdateState.Error -> Text(
                                text = stringResource(R.string.about_update_error),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                        }
                    },
                    onClick = ::checkForUpdates,
                ),
                ActionListItem(
                    labelRes = R.string.about_github,
                    onClick = { onOpenExternalUrl(TuneGithubUrl) },
                ),
                ActionListItem(
                    labelRes = R.string.about_star,
                    leadingSymbol = MaterialSymbols.Star,
                    onClick = { onOpenExternalUrl(TuneGithubUrl) },
                ),
                ActionListItem(
                    labelRes = R.string.about_license,
                    onClick = { onOpenExternalUrl(TuneLicenseUrl) },
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
            dividerStyle = ActionListDividerStyle.FullWidth,
        )
        when (val state = updateState) {
            is AboutUpdateState.Available -> Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Text(
                    text = state.release.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textMain,
                )
                Text(
                    text = updateNotesExcerpt(state.release.notes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TunePillButton(
                    label = stringResource(R.string.about_update_download_install),
                    onClick = { downloadAndInstall(state.release) },
                    variant = TunePillButtonVariant.Primary,
                    enabled = !downloading,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (downloading) {
                    Text(
                        text = stringResource(R.string.about_update_downloading),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            is AboutUpdateState.NotesOnly -> Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Text(
                    text = state.release.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textMain,
                )
                Text(
                    text = updateNotesExcerpt(state.release.notes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            AboutUpdateState.Checking -> Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_update_checking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
            AboutUpdateState.Error -> Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_update_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                TunePillButton(
                    label = stringResource(R.string.about_update_check),
                    onClick = ::checkForUpdates,
                    variant = TunePillButtonVariant.Secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            else -> Unit
        }
    }
}

internal fun updateNotesExcerpt(notes: String, maxChars: Int = 400): String {
    val trimmed = notes.trim()
    if (trimmed.isEmpty()) return trimmed
    val firstLines = trimmed.lines().take(6).joinToString("\n").trim()
    return if (firstLines.length <= maxChars) firstLines else firstLines.take(maxChars).trimEnd() + "…"
}

private fun awaitDownloadSuccess(context: Context, downloadId: Long): Boolean {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    repeat(600) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        runCatching {
            manager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED
                    if (status == DownloadManager.STATUS_SUCCESSFUL) return true
                    if (status == DownloadManager.STATUS_FAILED) return false
                }
            }
        }
        try {
            Thread.sleep(500)
        } catch (_: InterruptedException) {
            return false
        }
    }
    return false
}
