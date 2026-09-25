package com.exodidio.tune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.BuildConfig
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListDividerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.LabeledCard
import com.exodidio.tune.ui.theme.LocalTuneColors

private const val TuneGithubUrl = "https://github.com/Exodi-dio/tune"
private const val TuneLicenseUrl = "https://github.com/Exodi-dio/tune/blob/master/LICENSE"
private const val TuneGithubSponsorsUrl = "https://github.com/sponsors/exodidio"
private const val TuneKofiUrl = "https://ko-fi.com/exodidio"
private const val TuneBuyMeACoffeeUrl = "https://buymeacoffee.com/exodidio2"
private const val TunePatreonUrl = "https://www.patreon.com/c/exodidio"

@Composable
internal fun AboutContent(
    onOpenExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
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
                    labelRes = R.string.about_github,
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
        LabeledCard(
            label = stringResource(R.string.about_sponsor),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            ActionList(
                items = listOf(
                    ActionListItem(
                        labelRes = R.string.about_sponsor_github,
                        onClick = { onOpenExternalUrl(TuneGithubSponsorsUrl) },
                    ),
                    ActionListItem(
                        labelRes = R.string.about_sponsor_kofi,
                        onClick = { onOpenExternalUrl(TuneKofiUrl) },
                    ),
                    ActionListItem(
                        labelRes = R.string.about_sponsor_bmac,
                        onClick = { onOpenExternalUrl(TuneBuyMeACoffeeUrl) },
                    ),
                    ActionListItem(
                        labelRes = R.string.about_sponsor_patreon,
                        onClick = { onOpenExternalUrl(TunePatreonUrl) },
                    ),
                ),
                containerStyle = ActionListContainerStyle.Plain,
                dividerStyle = ActionListDividerStyle.FullWidth,
            )
        }
    }
}
