package com.exodidio.tune.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.exodidio.tune.library.stageArtistArtwork
import com.exodidio.tune.sync.AndroidLibrarySyncStore
import com.exodidio.tune.sync.ArtistArtworkOperation
import com.exodidio.tune.sync.ArtistArtworkPayload
import com.exodidio.tune.sync.ArtistMutation
import com.exodidio.tune.sync.LibraryArtist
import com.exodidio.tune.ui.libraryAlphabeticalComparator

enum class ArtistSortOption {
    Name,
    DateAdded,
}

data class LibraryArtistsUiState(
    val isLoaded: Boolean = true,
    val artists: List<LibraryArtist> = emptyList(),
    val filterQuery: String = "",
    val sortOption: ArtistSortOption = ArtistSortOption.Name,
    val sortOrder: SortOrder = SortOrder.Ascending,
    val artistArtworkKeys: Map<String, String> = emptyMap(),
    val artworkPathByKey: Map<String, String> = emptyMap(),
)

internal class LibraryArtistsViewModel(
    private val context: Context,
    syncStore: AndroidLibrarySyncStore,
) : ViewModel() {
    class Factory(private val context: Context, private val syncStore: AndroidLibrarySyncStore) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryArtistsViewModel(context, syncStore) as T
        }
    }

    private val syncStore = syncStore
    private val sortOptionFlow = MutableStateFlow(ArtistSortOption.Name)
    private val sortOrderFlow = MutableStateFlow(SortOrder.Ascending)
    private val filterQueryFlow = MutableStateFlow("")
    private val appliedFilterQuery = filterQueryFlow.debouncedLibrarySearchQuery()

    val uiState: StateFlow<LibraryArtistsUiState> = combine(
        combine(
            syncStore.artists,
            sortOptionFlow,
            sortOrderFlow,
            filterQueryFlow,
            appliedFilterQuery,
        ) { artists, option, order, query, appliedQuery ->
            ArtistListBasis(
                artists = sortArtists(artists.filter { matchesLibraryTextFilter(appliedQuery, it.name) }, option, order),
                filterQuery = query,
                sortOption = option,
                sortOrder = order,
            )
        },
        syncStore.artistArtworkKeys,
        syncStore.artworkPaths,
    ) { basis, artworkKeys, artworkPaths ->
        LibraryArtistsUiState(
            isLoaded = true,
            artists = basis.artists.map { artist ->
                artist.copy(artworkPath = artistDisplayArtworkPath(artist, artworkKeys, artworkPaths))
            },
            filterQuery = basis.filterQuery,
            sortOption = basis.sortOption,
            sortOrder = basis.sortOrder,
            artistArtworkKeys = artworkKeys,
            artworkPathByKey = artworkPaths,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryArtistsUiState(isLoaded = false),
    )

    fun setSortOption(option: ArtistSortOption) {
        sortOptionFlow.value = option
    }

    fun setFilterQuery(query: String) {
        filterQueryFlow.value = query
    }

    fun toggleSortOrder() {
        sortOrderFlow.value = if (sortOrderFlow.value == SortOrder.Ascending) {
            SortOrder.Descending
        } else {
            SortOrder.Ascending
        }
    }

    fun updateArtistArtwork(artistId: String, artworkUri: Uri? = null, clearArtwork: Boolean = false) {
        if (artistId.isBlank()) return
        viewModelScope.launch {
            val updatedAt = System.currentTimeMillis()
            artworkUri?.let { uri ->
                val staged = try {
                    withContext(Dispatchers.IO) { stageArtistArtwork(context.contentResolver, context.filesDir, uri) }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    Log.w("LibraryArtists", "Ignoring unreadable artist artwork", error)
                    null
                }
                staged?.let {
                    syncStore.stageArtistArtwork(it)
                    syncStore.queueArtistArtworkMutation(
                        ArtistMutation(UUID.randomUUID().toString(), artistId, ArtistArtworkOperation.SET_ARTWORK, updatedAt, ArtistArtworkPayload(artworkSha256 = it.sha256)),
                    )
                }
            }
            if (clearArtwork) {
                syncStore.queueArtistArtworkMutation(
                    ArtistMutation(UUID.randomUUID().toString(), artistId, ArtistArtworkOperation.REMOVE_ARTWORK, updatedAt),
                )
            }
        }
    }
}

private data class ArtistListBasis(
    val artists: List<LibraryArtist>,
    val filterQuery: String,
    val sortOption: ArtistSortOption,
    val sortOrder: SortOrder,
)

internal fun artistManualArtworkPath(
    artistId: String,
    artistArtworkKeys: Map<String, String>,
    artworkPaths: Map<String, String>,
): String? = artistArtworkKeys[artistId]?.let(artworkPaths::get)

internal fun artistDisplayArtworkPath(
    artist: LibraryArtist,
    artistArtworkKeys: Map<String, String>,
    artworkPaths: Map<String, String>,
): String? = artistManualArtworkPath(artist.id, artistArtworkKeys, artworkPaths) ?: artist.artworkPath

internal fun sortArtists(
    artists: List<LibraryArtist>,
    option: ArtistSortOption,
    order: SortOrder,
): List<LibraryArtist> {
    val comparator = when (option) {
        ArtistSortOption.Name -> compareBy<LibraryArtist, String>(libraryAlphabeticalComparator) { it.sortName }
            .thenBy { it.id }
        ArtistSortOption.DateAdded -> compareBy<LibraryArtist> { it.createdAt }
            .thenBy(libraryAlphabeticalComparator) { it.sortName }
            .thenBy { it.id }
    }
    val sorted = artists.sortedWith(comparator)
    return if (order == SortOrder.Descending) sorted.reversed() else sorted
}
