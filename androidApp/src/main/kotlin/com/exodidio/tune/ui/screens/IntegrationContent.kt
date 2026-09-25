package com.exodidio.tune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.lastfm.LastFmStatus
import com.exodidio.tune.lyrics.LyricsSettings
import com.exodidio.tune.lyrics.LyricsSource
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListDividerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.LabeledCard
import com.exodidio.tune.ui.components.Selection
import com.exodidio.tune.ui.components.SelectionOption
import com.exodidio.tune.ui.components.TunePillButton
import com.exodidio.tune.ui.components.TunePillButtonVariant
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.rememberArtworkThumbnail
import com.exodidio.tune.ui.theme.LocalTuneColors

@Composable
internal fun IntegrationContent(
    onLastFmSelected: () -> Unit,
    onLyricsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ActionList(
            items = listOf(
                ActionListItem(R.string.lastfm_title, onClick = onLastFmSelected),
                ActionListItem(R.string.lyrics_title, onClick = onLyricsSelected),
            ),
            containerStyle = ActionListContainerStyle.Card,
            dividerStyle = ActionListDividerStyle.FullWidth,
        )
    }
}

@Composable
internal fun LastFmContent(
    status: LastFmStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    val avatar = rememberArtworkThumbnail(status.avatarPath)
    Column(modifier = modifier) {
        HeroCard(
            title = stringResource(R.string.lastfm_title),
            description = if (status.connected) {
                stringResource(R.string.lastfm_connected_as, status.username)
            } else {
                stringResource(R.string.lastfm_description)
            },
            bottomContent = {
                TunePillButton(
                    label = stringResource(
                        when {
                            status.working -> R.string.lastfm_connecting
                            status.connected -> R.string.lastfm_disconnect
                            else -> R.string.lastfm_connect
                        },
                    ),
                    onClick = if (status.connected) onDisconnect else onConnect,
                    enabled = status.configured && !status.working,
                    variant = if (status.connected) TunePillButtonVariant.Secondary else TunePillButtonVariant.Primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                )
            },
        ) {
            if (status.connected && avatar != null) {
                Image(
                    bitmap = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).testTag("lastfm-avatar"),
                )
            } else {
                MaterialSymbol(
                    symbol = MaterialSymbols.GraphicEq,
                    contentDescription = null,
                    size = 40.dp,
                    tint = colors.textMuted,
                    modifier = Modifier.testTag("lastfm-icon"),
                )
            }
        }
        if (!status.configured || status.failed) {
            Text(
                text = stringResource(if (status.configured) R.string.lastfm_error else R.string.lastfm_not_configured),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
internal fun LyricsContent(
    settings: LyricsSettings,
    onSourceChanged: (LyricsSource) -> Unit,
    onLrclibChanged: (Boolean) -> Unit,
    onKugouChanged: (Boolean) -> Unit,
    onRomanizationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LabeledCard(label = stringResource(R.string.lyrics_data_sources), modifier = modifier) {
        Selection(
            labelRes = R.string.lyrics_preferred_source,
            options = listOf(
                SelectionOption(LyricsSource.Desktop, R.string.lyrics_source_desktop),
                SelectionOption(LyricsSource.AutoFetch, R.string.lyrics_source_auto_fetch),
            ),
            selectedValue = settings.preferredSource,
            onValueSelected = onSourceChanged,
        )
        com.exodidio.tune.ui.components.ActionListDivider(style = ActionListDividerStyle.FullWidth)
        ActionList(
            items = listOf(
                ActionListItem(R.string.lyrics_lrclib, trailingContent = { Switch(checked = settings.lrclib, onCheckedChange = onLrclibChanged) }, onClick = { onLrclibChanged(!settings.lrclib) }),
                ActionListItem(R.string.lyrics_kugou, trailingContent = { Switch(checked = settings.kugou, onCheckedChange = onKugouChanged) }, onClick = { onKugouChanged(!settings.kugou) }),
                ActionListItem(R.string.lyrics_romanization, trailingContent = { Switch(checked = settings.romanizationEnabled, onCheckedChange = onRomanizationEnabledChanged) }, onClick = { onRomanizationEnabledChanged(!settings.romanizationEnabled) }),
            ),
            containerStyle = ActionListContainerStyle.Plain,
            dividerStyle = ActionListDividerStyle.FullWidth,
        )
    }
}
