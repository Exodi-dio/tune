package com.exodidio.tune.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.exodidio.tune.AppDestination
import com.exodidio.tune.ui.navigation.placeholderRes
import com.exodidio.tune.ui.theme.LocalTuneColors

@Composable
internal fun PlaceholderContent(destination: AppDestination, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(destination.placeholderRes),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = LocalTuneColors.current.textMuted,
    )
}
