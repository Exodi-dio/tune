package com.exodidio.tune.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.MaterialSymbols

@Composable
internal fun SettingsContent(
    onAppearanceSelected: () -> Unit,
    onPlaybackSelected: () -> Unit,
    onIntegrationSelected: () -> Unit,
    onMusicSyncSelected: () -> Unit,
    onAboutSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ActionList(
            items = listOf(
                ActionListItem(
                    R.string.settings_appearance,
                    leadingSymbol = MaterialSymbols.Palette,
                    onClick = onAppearanceSelected,
                ),
                ActionListItem(
                    R.string.settings_playback,
                    leadingSymbol = MaterialSymbols.Subwoofer,
                    onClick = onPlaybackSelected,
                ),
                ActionListItem(
                    R.string.settings_integration,
                    leadingSymbol = MaterialSymbols.Power,
                    onClick = onIntegrationSelected,
                ),
                ActionListItem(
                    R.string.settings_music_sync,
                    leadingSymbol = MaterialSymbols.Sync,
                    onClick = onMusicSyncSelected,
                ),
                ActionListItem(
                    R.string.settings_about,
                    leadingSymbol = MaterialSymbols.Info,
                    onClick = onAboutSelected,
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
        )
    }
}
