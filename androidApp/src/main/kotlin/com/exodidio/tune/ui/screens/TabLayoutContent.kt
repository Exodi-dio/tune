package com.exodidio.tune.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.AppDestination
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.navigation.titleRes
import com.exodidio.tune.ui.theme.LocalTuneColors

/** Settings page to reorder and show/hide bottom-navigation destinations. */
@Composable
internal fun TabLayoutContent(
    visibleTabs: List<AppDestination>,
    onVisibleTabsChanged: (List<AppDestination>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    var tabs by remember { mutableStateOf(visibleTabs.toMutableList()) }
    var visible by remember { mutableStateOf(visibleTabs.toSet()) }

    // Re-sync local state when the preferred tabs change externally.
    LaunchedEffect(visibleTabs) {
        tabs = visibleTabs.toMutableList()
        visible = visibleTabs.toSet()
    }

    val effectiveTabs = tabs.filter { visible.contains(it) }

    LaunchedEffect(effectiveTabs) {
        onVisibleTabsChanged(effectiveTabs)
    }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target in tabs.indices) {
            val item = tabs.removeAt(index)
            tabs.add(target, item)
        }
    }

    fun toggleVisible(dest: AppDestination, checked: Boolean) {
        if (checked) visible += dest else visible -= dest
    }

    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.tab_layout_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ActionList(
            items = tabs.withIndex().map { (index, dest) ->
                ActionListItem(
                    labelRes = dest.titleRes,
                    leadingSymbol = dest.symbol,
                    leadingIconTint = colors.primary,
                    trailingContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Switch(
                                checked = visible.contains(dest),
                                onCheckedChange = { toggleVisible(dest, it) },
                            )
                            IconButton(
                                onClick = { move(index, -1) },
                                enabled = index > 0,
                            ) {
                                MaterialSymbol(
                                    symbol = MaterialSymbols.ArrowUpward,
                                    size = 20.dp,
                                    tint = colors.textMuted,
                                )
                            }
                            IconButton(
                                onClick = { move(index, +1) },
                                enabled = index < tabs.lastIndex,
                            ) {
                                MaterialSymbol(
                                    symbol = MaterialSymbols.ArrowDownward,
                                    size = 20.dp,
                                    tint = colors.textMuted,
                                )
                            }
                        }
                    },
                    onClick = {},
                )
            },
            containerStyle = ActionListContainerStyle.Card,
        )
    }
}
