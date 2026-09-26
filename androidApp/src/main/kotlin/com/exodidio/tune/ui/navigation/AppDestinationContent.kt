package com.exodidio.tune.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.lazy.LazyListState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.exodidio.tune.AppDestination
import com.exodidio.tune.AppDestinationModels
import com.exodidio.tune.AppIntent
import com.exodidio.tune.AppStackPage
import com.exodidio.tune.StackPageEntry
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.components.HomeContent
import com.exodidio.tune.ui.components.StackPageLayout
import com.exodidio.tune.ui.screens.AboutContent
import com.exodidio.tune.ui.screens.AppearanceContent
import com.exodidio.tune.ui.screens.LibraryContent
import com.exodidio.tune.ui.screens.LibrarySearchContent
import com.exodidio.tune.ui.screens.LibrarySearchUiState
import com.exodidio.tune.ui.screens.InsightContent
import com.exodidio.tune.ui.screens.InsightPeriod
import com.exodidio.tune.ui.screens.InsightUiState
import com.exodidio.tune.ui.screens.SettingsContent
import com.exodidio.tune.ui.screens.IntegrationContent
import com.exodidio.tune.ui.screens.LastFmContent
import com.exodidio.tune.ui.screens.LyricsContent
import com.exodidio.tune.ui.screens.MusicSyncContent
import com.exodidio.tune.ui.screens.PlaybackSettingsContent
import com.exodidio.tune.ui.screens.VolumeNormalizationContent
import com.exodidio.tune.ui.screens.SongTransitionContent
import com.exodidio.tune.ui.screens.EqualizerContent
import com.exodidio.tune.lastfm.LastFmStatus
import com.exodidio.tune.ui.theme.LocalTuneColors

import com.exodidio.tune.ui.screens.LibraryTracksContent
import com.exodidio.tune.ui.screens.LibraryTracksUiState
import com.exodidio.tune.ui.screens.HomeUiState
import com.exodidio.tune.ui.screens.TrackSortOption
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.ui.screens.LibraryArtistsContent
import com.exodidio.tune.ui.screens.LibraryArtistsUiState
import com.exodidio.tune.ui.screens.AlbumSortOption
import com.exodidio.tune.ui.screens.LibraryAlbumsContent
import com.exodidio.tune.ui.screens.LibraryAlbumsUiState
import com.exodidio.tune.ui.screens.LibraryGenresContent
import com.exodidio.tune.ui.screens.LibraryGenresUiState
import com.exodidio.tune.ui.screens.LibraryComposersContent
import com.exodidio.tune.ui.screens.LibraryComposersUiState
import com.exodidio.tune.ui.screens.LibraryPlaylistsContent
import com.exodidio.tune.ui.screens.LibraryPlaylistsUiState
import com.exodidio.tune.ui.screens.PlaylistDetailsContent
import com.exodidio.tune.ui.screens.PlaylistDetailsUiState
import com.exodidio.tune.ui.screens.playlistDetailsUiStateFor
import com.exodidio.tune.ui.screens.AlbumDetailsContent
import com.exodidio.tune.ui.screens.AlbumDetailsUiState
import com.exodidio.tune.ui.screens.albumDetailsUiStateFor
import com.exodidio.tune.ui.screens.ArtistDetailsContent
import com.exodidio.tune.ui.screens.ArtistDetailsUiState
import com.exodidio.tune.ui.screens.artistDetailsUiStateFor
import com.exodidio.tune.ui.screens.GenreDetailsContent
import com.exodidio.tune.ui.screens.GenreDetailsUiState
import com.exodidio.tune.ui.screens.genreDetailsUiStateFor
import com.exodidio.tune.ui.screens.ComposerDetailsContent
import com.exodidio.tune.ui.screens.ComposerDetailsUiState
import com.exodidio.tune.ui.screens.composerDetailsUiStateFor
import com.exodidio.tune.sync.LibraryGenre
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection

internal typealias PageKey = StackPageEntry

internal enum class ContentScrollDirection {
    Up,
    Down,
}

internal data class ContentScrollDelta(
    val direction: ContentScrollDirection,
    val distancePx: Float,
)

internal fun contentScrollDelta(consumedY: Float): ContentScrollDelta? = when {
    consumedY < 0f -> ContentScrollDelta(ContentScrollDirection.Up, -consumedY)
    consumedY > 0f -> ContentScrollDelta(ContentScrollDirection.Down, consumedY)
    else -> null
}

@Composable
internal fun AppDestinationContent(
    stackPage: StackPageEntry,
    themeMode: ThemeMode,
    reduceTransparency: Boolean,
    hazeState: HazeState?,
    navigationBottomPadding: Dp,
    homeListState: LazyListState,
    insightListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    libraryListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    tracksListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    artistsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    albumsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    artistDetailsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    genreDetailsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    composerDetailsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    genresListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    composersListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    playlistsListState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    selectedAlbumId: String? = null,
    selectedPlaylistId: String? = null,
    selectedArtistId: String? = null,
    selectedGenreId: String? = null,
    selectedComposerId: String? = null,
    onIntent: (AppIntent) -> Unit,
    destinations: AppDestinationModels,
    playbackQueue: PlaybackQueueSnapshot,
    onTrackPlayNext: (String) -> Unit,
    onTrackAddToQueue: (String) -> Unit,
    onTrackFavoriteToggle: (String, Boolean) -> Unit,
    onTrackContextBottomSheet: (com.exodidio.tune.ui.components.TrackContextBottomSheetRequest) -> Unit = {},
    onAlbumHeroColorChanged: (Color) -> Unit = {},
    onSettingsContentScrolled: (Boolean) -> Unit = {},
    onContentScroll: (ContentScrollDelta) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val homeUiState = destinations.home.state
    val insightUiState = destinations.insight.state
    val library = destinations.library
    val tracksUiState = library.tracks.state
    val artistsUiState = library.artists.state
    val albumsUiState = library.albums.state
    val genresUiState = library.genres.state
    val composersUiState = library.composers.state
    val playlistsUiState = library.playlists.state
    val searchUiState = library.search.state
    val albumDetailsUiState = library.details.albums
    val playlistDetailsUiState = library.details.playlists
    val artistDetailsUiState = library.details.artists
    val genreDetailsUiState = library.details.genres
    val composerDetailsUiState = library.details.composers
    val selectedAlbumDetails = remember(albumDetailsUiState, selectedAlbumId) {
        selectedAlbumId?.let { albumDetailsUiStateFor(albumDetailsUiState, it) } ?: AlbumDetailsUiState()
    }
    val selectedPlaylistDetails = remember(playlistDetailsUiState, selectedPlaylistId) {
        selectedPlaylistId?.let { playlistDetailsUiStateFor(playlistDetailsUiState, it) } ?: PlaylistDetailsUiState()
    }
    val selectedArtistDetails = remember(artistDetailsUiState, selectedArtistId) {
        selectedArtistId?.let { artistDetailsUiStateFor(artistDetailsUiState, it) } ?: ArtistDetailsUiState()
    }
    val selectedGenreDetails = remember(genreDetailsUiState, selectedGenreId) {
        selectedGenreId?.let { genreDetailsUiStateFor(genreDetailsUiState, it) } ?: GenreDetailsUiState()
    }
    val selectedComposerDetails = remember(composerDetailsUiState, selectedComposerId) {
        selectedComposerId?.let { composerDetailsUiStateFor(composerDetailsUiState, it) } ?: ComposerDetailsUiState()
    }
    val settings = destinations.settings
    val onHomeTrackClick = destinations.home.onTrackClick
    val onInsightLibraryPeriodSelected = destinations.insight.onLibraryPeriodSelected
    val onInsightListeningPeriodSelected = destinations.insight.onListeningPeriodSelected
    val onInsightTrackClick = destinations.insight.onTrackClick
    val onSortOptionSelected = library.tracks.onSortOptionSelected
    val onToggleSortOrder = library.tracks.onToggleSortOrder
    val onTrackClick = library.tracks.onTrackClick
    val onTracksPlayAll = library.tracks.onPlayAll
    val onTracksFilterQueryChange = library.tracks.onFilterQueryChange
    val onRecentTrackClick = library.tracks.onRecentTrackClick
    val onSearchQueryChange = library.search.onQueryChange
    val onSearchTrackClick = library.search.onTrackClick
    val onAlbumPlayNext = library.albums.onPlayNext
    val onAlbumAddToQueue = library.albums.onAddToQueue
    val onAlbumAddToFavorites = library.albums.onAddToFavorites
    val onAlbumPlay = library.albums.onPlay
    val onAlbumsPlayAll = library.albums.onPlayAll
    val onAlbumsFilterQueryChange = library.albums.onFilterQueryChange
    val onAlbumTrackPlay = library.albums.onTrackPlay
    val onPlaylistPlay = library.playlists.onPlay
    val onPlaylistTrackPlay = library.playlists.onTrackPlay
    val onPlaylistTrackRemove = library.playlists.onTrackRemove
    val onPlaylistTrackMove = library.playlists.onTrackMove
    val onPlaylistPlayNext = library.playlists.onPlayNext
    val onPlaylistAddToQueue = library.playlists.onAddToQueue
    val onPlaylistUpdate = library.playlists.onUpdate
    val onPlaylistDelete = library.playlists.onDelete
    val onArtistPlay = library.artists.onPlay
    val onArtistPlayNext = library.artists.onPlayNext
    val onArtistAddToQueue = library.artists.onAddToQueue
    val orderedTrackIdsForArtist = library.artists.orderedTrackIds
    val onArtistsFilterQueryChange = library.artists.onFilterQueryChange
    val onGenrePlay = library.genres.onPlay
    val onGenrePlayNext = library.genres.onPlayNext
    val onGenreAddToQueue = library.genres.onAddToQueue
    val orderedTrackIdsForGenre = library.genres.orderedTrackIds
    val onGenresFilterQueryChange = library.genres.onFilterQueryChange
    val onComposerPlay = library.composers.onPlay
    val onComposerPlayNext = library.composers.onPlayNext
    val onComposerAddToQueue = library.composers.onAddToQueue
    val orderedTrackIdsForComposer = library.composers.orderedTrackIds
    val onComposersFilterQueryChange = library.composers.onFilterQueryChange
    val onArtistTrackContextBottomSheet = onTrackContextBottomSheet
    val onGenreTrackContextBottomSheet = onTrackContextBottomSheet
    val onComposerTrackContextBottomSheet = onTrackContextBottomSheet
    val lastFmStatus = settings.lastFmStatus
    val onLastFmConnect = settings.onLastFmConnect
    val onLastFmDisconnect = settings.onLastFmDisconnect
    val lyricsSettings = settings.lyricsSettings
    val onLrclibChanged = settings.onLrclibChanged
    val onKugouChanged = settings.onKugouChanged
    val onRomanizationEnabledChanged = settings.onRomanizationEnabledChanged
    val onLyricsSourceChanged = settings.onLyricsSourceChanged
    val crossfadeSeconds = settings.crossfadeSeconds
    val lastEnabledCrossfadeSeconds = settings.lastEnabledCrossfadeSeconds
    val onCrossfadeSecondsChanged = settings.onCrossfadeSecondsChanged
    val blendArtworkDuringCrossfade = settings.blendArtworkDuringCrossfade
    val onBlendArtworkDuringCrossfadeChanged = settings.onBlendArtworkDuringCrossfadeChanged
    val showFullscreenQualityBadge = settings.showFullscreenQualityBadge
    val onShowFullscreenQualityBadgeChanged = settings.onShowFullscreenQualityBadgeChanged
    val normalizationAvailable = settings.normalizationAvailable
    val normalization = settings.normalization
    val onNormalizationChanged = settings.onNormalizationChanged
    val equalizer = settings.equalizer
    val onEqualizerEnabledChanged = settings.onEqualizerEnabledChanged
    val onEqualizerPresetSelected = settings.onEqualizerPresetSelected
    val onEqualizerBandChanged = settings.onEqualizerBandChanged
    val colors = LocalTuneColors.current
    val destination = stackPage.destination
    val page = stackPage.page
    val currentOnContentScroll = rememberUpdatedState(onContentScroll)
    val currentOnSettingsContentScrolled = rememberUpdatedState(onSettingsContentScrolled)
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    contentScrollDelta(consumed.y)?.let(currentOnContentScroll.value)
                }
                return Offset.Zero
            }
        }
    }
    Surface(
        color = colors.background,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
            .then(if (hazeState == null) Modifier else Modifier.hazeSource(hazeState)),
    ) {
        StackPageLayout(
            title = stringResource(page.titleRes(destination)),
            hazeState = hazeState,
            contentBottomPadding = navigationBottomPadding,
            isContentScrolled = false,
            onBackClick = if (page != AppStackPage.Root) {
                { onIntent(AppIntent.NavigateBack) }
            } else {
                null
            },
            showHeader = false,
        ) { modifier, contentPadding ->
            AnimatedContent(
                targetState = stackPage,
                modifier = modifier,
                transitionSpec = {
                    if (targetState.destination != initialState.destination) {
                        fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 200))
                    } else if (isForwardTransition(targetState, initialState)) {
                        (slideInHorizontally { it } togetherWith
                            slideOutHorizontally { -it / 4 }).apply {
                            targetContentZIndex = 1f
                        }
                    } else {
                        (slideInHorizontally { -it / 4 } togetherWith
                            slideOutHorizontally { it }).apply {
                            targetContentZIndex = 0f
                        }
                    }
                },
                label = "stack-page-content",
            ) { currentPage ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .pointerInput(focusManager) {
                            detectTapGestures { focusManager.clearFocus() }
                        },
                ) {
                    when (currentPage.destination) {
                        AppDestination.Home -> HomeDestinationContent {
                            if (currentPage.page == AppStackPage.Root) {
                            HomeContent(
                                modifier = Modifier.fillMaxSize(),
                                listState = homeListState,
                                contentPadding = contentPadding,
                                isLoaded = homeUiState.isLoaded,
                                keepListeningTracks = homeUiState.keepListeningTracks,
                                mostPlayedTracks = homeUiState.mostPlayedTracks,
                                forgottenTracks = homeUiState.forgottenTracks,
                                onTrackClick = onHomeTrackClick,
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = { track -> onTrackPlayNext(track.id) },
                                onTrackAddToQueue = { track -> onTrackAddToQueue(track.id) },
                                onTrackFavoriteToggle = { track, favorite -> onTrackFavoriteToggle(track.id, favorite) },
                                onTrackAlbumClick = { track -> onIntent(AppIntent.OpenAlbumDetails(track.albumId)) },
                                onTrackArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                            )
                            }
                        }
                        AppDestination.Insight -> InsightDestinationContent {
                            InsightContent(
                                state = insightUiState,
                                listState = insightListState,
                                contentPadding = contentPadding,
                                onLibraryPeriodSelected = onInsightLibraryPeriodSelected,
                                onListeningPeriodSelected = onInsightListeningPeriodSelected,
                                onArtistClick = { onIntent(AppIntent.OpenArtistDetails(it)) },
                                onTrackClick = onInsightTrackClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        AppDestination.Settings -> SettingsDestinationContent {
                            key(currentPage.page) {
                            val settingsScrollState = rememberScrollState()
                            LaunchedEffect(currentPage, stackPage, settingsScrollState.value) {
                                if (currentPage == stackPage) {
                                    currentOnSettingsContentScrolled.value(settingsScrollState.value > 0)
                                }
                            }
                            val layoutDirection = LocalLayoutDirection.current
                            val settingsPageModifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(settingsScrollState)
                                .padding(
                                    start = contentPadding.calculateStartPadding(layoutDirection),
                                    top = contentPadding.calculateTopPadding(),
                                    end = contentPadding.calculateEndPadding(layoutDirection),
                                    bottom = contentPadding.calculateBottomPadding(),
                                )
                                .testTag("settings-page-scroll")
                            when (currentPage.page) {
                            AppStackPage.SettingsAppearance -> AppearanceContent(
                                modifier = settingsPageModifier,
                                themeMode = themeMode,
                                reduceTransparency = reduceTransparency,
                                onThemeModeSelected = { themeMode ->
                                    onIntent(AppIntent.SetThemeMode(themeMode))
                                },
                                onReduceTransparencyChanged = { enabled ->
                                    onIntent(AppIntent.SetReduceTransparency(enabled))
                                },
                                hazeState = hazeState,
                            )
                            AppStackPage.SettingsPlayback -> PlaybackSettingsContent(
                                showFullscreenQualityBadge = showFullscreenQualityBadge,
                                onShowFullscreenQualityBadgeChanged = onShowFullscreenQualityBadgeChanged,
                                onSongTransitionSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsSongTransition))
                                },
                                onVolumeNormalizationSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsVolumeNormalization))
                                },
                                onEqualizerSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsEqualizer))
                                },
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsSongTransition -> SongTransitionContent(
                                crossfadeSeconds = crossfadeSeconds,
                                lastEnabledCrossfadeSeconds = lastEnabledCrossfadeSeconds,
                                onCrossfadeSecondsChanged = onCrossfadeSecondsChanged,
                                blendArtworkDuringCrossfade = blendArtworkDuringCrossfade,
                                onBlendArtworkDuringCrossfadeChanged = onBlendArtworkDuringCrossfadeChanged,
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsVolumeNormalization -> VolumeNormalizationContent(
                                normalizationAvailable = normalizationAvailable,
                                normalization = normalization,
                                onNormalizationChanged = onNormalizationChanged,
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsEqualizer -> EqualizerContent(
                                settings = equalizer,
                                onEnabledChanged = onEqualizerEnabledChanged,
                                onPresetSelected = onEqualizerPresetSelected,
                                onBandChanged = onEqualizerBandChanged,
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsIntegration -> IntegrationContent(
                                onLastFmSelected = { onIntent(AppIntent.OpenPage(AppStackPage.SettingsLastFm)) },
                                onLyricsSelected = { onIntent(AppIntent.OpenPage(AppStackPage.SettingsLyrics)) },
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsMusicSync -> MusicSyncContent(
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsLastFm -> LastFmContent(
                                status = lastFmStatus,
                                onConnect = onLastFmConnect,
                                onDisconnect = onLastFmDisconnect,
                                modifier = settingsPageModifier,
                            )
                            AppStackPage.SettingsLyrics -> LyricsContent(lyricsSettings, onLyricsSourceChanged, onLrclibChanged, onKugouChanged, onRomanizationEnabledChanged, settingsPageModifier)
                            AppStackPage.SettingsAbout -> AboutContent(
                                modifier = settingsPageModifier,
                                onOpenExternalUrl = { url ->
                                    onIntent(AppIntent.OpenExternalUrl(url))
                                },
                            )
                            else -> SettingsContent(
                                modifier = settingsPageModifier,
                                onAppearanceSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsAppearance))
                                },
                                onPlaybackSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsPlayback))
                                },
                                onIntegrationSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsIntegration))
                                },
                                onMusicSyncSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsMusicSync))
                                },
                                onAboutSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsAbout))
                                },
                            )
                            }
                            }
                        }
                        AppDestination.Library -> LibraryDestinationContent {
                            when (currentPage.page) {
                            AppStackPage.LibrarySearch -> LibrarySearchContent(
                                uiState = searchUiState,
                                contentPadding = contentPadding,
                                onQueryChange = onSearchQueryChange,
                                onTrackClick = onSearchTrackClick,
                                onAlbumClick = { onIntent(AppIntent.OpenAlbumDetails(it)) },
                                onArtistClick = { onIntent(AppIntent.OpenArtistDetails(it)) },
                                onPlaylistClick = { onIntent(AppIntent.OpenPlaylistDetails(it)) },
                                onComposerClick = { onIntent(AppIntent.OpenComposerDetails(it)) },
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = { onTrackPlayNext(it.id) },
                                onTrackAddToQueue = { onTrackAddToQueue(it.id) },
                                onTrackFavoriteToggle = { track, favorite -> onTrackFavoriteToggle(track.id, favorite) },
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                                onAlbumPlayNext = onAlbumPlayNext,
                                onAlbumAddToQueue = onAlbumAddToQueue,
                                onAlbumAddToFavorites = onAlbumAddToFavorites,
                            )
                            AppStackPage.LibraryArtists -> LibraryArtistsContent(
                                uiState = artistsUiState,
                                listState = artistsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                onFilterQueryChange = onArtistsFilterQueryChange,
                                orderedTrackIdsForArtist = orderedTrackIdsForArtist,
                                onArtistPlayNext = onArtistPlayNext,
                                onArtistAddToQueue = onArtistAddToQueue,
                                onTrackContextBottomSheet = onArtistTrackContextBottomSheet,
                                hazeState = hazeState,
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.ArtistDetails -> ArtistDetailsContent(
                                uiState = selectedArtistDetails,
                                listState = artistDetailsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                hazeState = hazeState,
                                reduceTransparency = reduceTransparency,
                                onHeroColorChanged = onAlbumHeroColorChanged,
                                onPlay = { selectedArtistId?.let { onArtistPlay(it, false) } },
                                onShuffle = { selectedArtistId?.let { onArtistPlay(it, true) } },
                                onPlayNext = onArtistPlayNext,
                                onAddToQueue = onArtistAddToQueue,
                                onTrackContextBottomSheet = onArtistTrackContextBottomSheet,
                                onAlbumClick = { album -> onIntent(AppIntent.OpenAlbumDetails(album.id)) },
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.GenreDetails -> GenreDetailsContent(
                                uiState = selectedGenreDetails,
                                listState = genreDetailsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                hazeState = hazeState,
                                reduceTransparency = reduceTransparency,
                                onHeroColorChanged = onAlbumHeroColorChanged,
                                onPlay = { selectedGenreId?.let { onGenrePlay(it, false) } },
                                onShuffle = { selectedGenreId?.let { onGenrePlay(it, true) } },
                                onPlayNext = onGenrePlayNext,
                                onAddToQueue = onGenreAddToQueue,
                                onTrackContextBottomSheet = onGenreTrackContextBottomSheet,
                                onAlbumClick = { album -> onIntent(AppIntent.OpenAlbumDetails(album.id)) },
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.ComposerDetails -> ComposerDetailsContent(
                                uiState = selectedComposerDetails,
                                listState = composerDetailsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                hazeState = hazeState,
                                reduceTransparency = reduceTransparency,
                                onHeroColorChanged = onAlbumHeroColorChanged,
                                onPlay = { selectedComposerId?.let { onComposerPlay(it, false) } },
                                onShuffle = { selectedComposerId?.let { onComposerPlay(it, true) } },
                                onPlayNext = onComposerPlayNext,
                                onAddToQueue = onComposerAddToQueue,
                                onTrackContextBottomSheet = onComposerTrackContextBottomSheet,
                                onAlbumClick = { album -> onIntent(AppIntent.OpenAlbumDetails(album.id)) },
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.LibraryAlbums -> LibraryAlbumsContent(
                                uiState = albumsUiState,
                                listState = albumsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onAlbumClick = { album -> onIntent(AppIntent.OpenAlbumDetails(album.id)) },
                                hazeState = hazeState,
                                playbackQueue = playbackQueue,
                                onAlbumPlay = onAlbumPlay,
                                onPlayAll = onAlbumsPlayAll,
                                onAlbumPlayNext = onAlbumPlayNext,
                                onAlbumAddToQueue = onAlbumAddToQueue,
                                onAlbumAddToFavorites = onAlbumAddToFavorites,
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                                onFilterQueryChange = onAlbumsFilterQueryChange,
                            )
                            AppStackPage.AlbumDetails -> AlbumDetailsContent(
                                uiState = selectedAlbumDetails,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                hazeState = hazeState,
                                reduceTransparency = reduceTransparency,
                                onHeroColorChanged = onAlbumHeroColorChanged,
                                onPlay = { selectedAlbumId?.let { onAlbumPlay(it, false) } },
                                onShuffle = { selectedAlbumId?.let { onAlbumPlay(it, true) } },
                                onTrackClick = { trackId -> selectedAlbumId?.let { onAlbumTrackPlay(it, trackId) } },
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = onTrackPlayNext,
                                onTrackAddToQueue = onTrackAddToQueue,
                                onTrackFavoriteToggle = onTrackFavoriteToggle,
                                onTrackArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                onAlbumPlayNext = onAlbumPlayNext,
                                onAlbumAddToQueue = onAlbumAddToQueue,
                                onAlbumAddToFavorites = onAlbumAddToFavorites,
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                            )
                            AppStackPage.LibraryGenres -> LibraryGenresContent(
                                uiState = genresUiState,
                                listState = genresListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onGenreClick = { genre -> onIntent(AppIntent.OpenGenreDetails(genre.id)) },
                                onFilterQueryChange = onGenresFilterQueryChange,
                                orderedTrackIdsForGenre = orderedTrackIdsForGenre,
                                onGenrePlayNext = onGenrePlayNext,
                                onGenreAddToQueue = onGenreAddToQueue,
                                onTrackContextBottomSheet = onGenreTrackContextBottomSheet,
                                hazeState = hazeState,
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.LibraryComposers -> LibraryComposersContent(
                                uiState = composersUiState,
                                listState = composersListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onComposerClick = { composer -> onIntent(AppIntent.OpenComposerDetails(composer.id)) },
                                onFilterQueryChange = onComposersFilterQueryChange,
                                orderedTrackIdsForComposer = orderedTrackIdsForComposer,
                                onComposerPlayNext = onComposerPlayNext,
                                onComposerAddToQueue = onComposerAddToQueue,
                                onTrackContextBottomSheet = onComposerTrackContextBottomSheet,
                                hazeState = hazeState,
                                playbackQueue = playbackQueue,
                            )
                            AppStackPage.LibraryPlaylists -> LibraryPlaylistsContent(
                                uiState = playlistsUiState,
                                listState = playlistsListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onPlaylistClick = { playlistId -> onIntent(AppIntent.OpenPlaylistDetails(playlistId)) },
                                onPlaylistPlayNext = onPlaylistPlayNext,
                                onPlaylistAddToQueue = onPlaylistAddToQueue,
                                onPlaylistUpdate = onPlaylistUpdate,
                                onPlaylistDelete = onPlaylistDelete,
                            )
                            AppStackPage.PlaylistDetails -> PlaylistDetailsContent(
                                uiState = selectedPlaylistDetails,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                hazeState = hazeState,
                                reduceTransparency = reduceTransparency,
                                onHeroColorChanged = onAlbumHeroColorChanged,
                                onPlay = { selectedPlaylistId?.let { onPlaylistPlay(it, false) } },
                                onShuffle = { selectedPlaylistId?.let { onPlaylistPlay(it, true) } },
                                onTrackClick = { trackId -> selectedPlaylistId?.let { onPlaylistTrackPlay(it, trackId) } },
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = onTrackPlayNext,
                                onTrackAddToQueue = onTrackAddToQueue,
                                onTrackFavoriteToggle = onTrackFavoriteToggle,
                                onTrackArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                onTrackRemoveFromPlaylist = { trackId -> selectedPlaylistId?.let { onPlaylistTrackRemove(it, trackId) } },
                                onTrackMove = { trackId, previousTrackId, nextTrackId -> selectedPlaylistId?.let { onPlaylistTrackMove(it, trackId, previousTrackId, nextTrackId) } },
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                                onPlaylistPlayNext = onPlaylistPlayNext,
                                onPlaylistAddToQueue = onPlaylistAddToQueue,
                                onPlaylistUpdate = onPlaylistUpdate,
                                onPlaylistDelete = { playlistId ->
                                    onPlaylistDelete(playlistId)
                                    onIntent(AppIntent.NavigateBack)
                                },
                            )
                            AppStackPage.LibraryTracks -> LibraryTracksContent(
                                uiState = tracksUiState,
                                onSortOptionSelected = onSortOptionSelected,
                                onToggleSortOrder = onToggleSortOrder,
                                listState = tracksListState,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                                onTrackClick = { track -> onTrackClick(track.id) },
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = { track -> onTrackPlayNext(track.id) },
                                onTrackAddToQueue = { track -> onTrackAddToQueue(track.id) },
                                onTrackFavoriteToggle = { track, favorite -> onTrackFavoriteToggle(track.id, favorite) },
                                onTrackAlbumClick = { track -> onIntent(AppIntent.OpenAlbumDetails(track.albumId)) },
                                onTrackArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                hazeState = hazeState,
                                onPlayAll = onTracksPlayAll,
                                onFilterQueryChange = onTracksFilterQueryChange,
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                            )
                            else -> LibraryContent(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                                recentTracks = tracksUiState.recentTracks,
                                listState = libraryListState,
                                onTrackClick = onRecentTrackClick,
                                playbackQueue = playbackQueue,
                                onTrackPlayNext = { track -> onTrackPlayNext(track.id) },
                                onTrackAddToQueue = { track -> onTrackAddToQueue(track.id) },
                                onTrackFavoriteToggle = { track, favorite -> onTrackFavoriteToggle(track.id, favorite) },
                                onTrackAlbumClick = { track -> onIntent(AppIntent.OpenAlbumDetails(track.albumId)) },
                                onTrackArtistClick = { artist -> onIntent(AppIntent.OpenArtistDetails(artist.id)) },
                                onTrackContextBottomSheet = onTrackContextBottomSheet,
                                onSearchSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibrarySearch))
                                },
                                onTracksSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryTracks))
                                },
                                onArtistsSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryArtists))
                                },
                                onAlbumsSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryAlbums))
                                },
                                onGenresSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryGenres))
                                },
                                onComposersSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryComposers))
                                },
                                onPlaylistsSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.LibraryPlaylists))
                                },
                            )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeDestinationContent(content: @Composable () -> Unit) = content()

@Composable
internal fun InsightDestinationContent(content: @Composable () -> Unit) = content()

@Composable
internal fun LibraryDestinationContent(content: @Composable () -> Unit) = content()

@Composable
internal fun SettingsDestinationContent(content: @Composable () -> Unit) = content()

internal fun isForwardTransition(target: PageKey, initial: PageKey): Boolean =
    target.index > initial.index
