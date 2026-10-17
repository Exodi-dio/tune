package com.exodidio.tune.sync

import android.content.Context
import com.exodidio.tune.library.LocalImport
import com.exodidio.tune.mood.MoodRadioTrack
import com.exodidio.tune.player.DailyPlaybackAttemptStat
import com.exodidio.tune.player.DailyTrackListeningStat
import com.exodidio.tune.player.TrackAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

/** ============================================================================
 *  Data classes recreated from deleted SyncDatabase.kt
 * ============================================================================ */

 data class LibraryTrack(
    val id: String = "",
    val title: String = "",
    val artists: String = "",
    val album: String = "",
    val albumId: String = "",
    val artworkKey: String? = null,
    val playCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val discNumber: Int = 0,
    val trackNumber: Int = 0,
    val syncOrder: Int = 0,
    val metadataJson: String = "{}",
    val artworkPath: String? = null,
    val audioPath: String? = null,
    val sortTitle: String = "",
    val sortArtists: String = "",
)

/** Row type used by sync mapper tests. */
 data class LibraryTrackRow(
    val id: String = "",
    val title: String = "",
    val artists: String = "",
    val album: String = "",
    val artworkKey: String? = null,
    val playCount: Int = 0,
    val createdAt: String = "",
    val artworkPath: String? = null,
    val audioPath: String? = null,
    val rawJson: String = "{}",
)

 data class LibraryPlaylist(
    val id: String = "",
    val name: String = "",
    val artworkKey: String? = null,
    val trackIds: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val sortTitle: String = "",
    val metadataJson: String = "{}",
)

 data class LibraryPlaylistRow(
    val id: String = "",
    val name: String = "",
    val artworkKey: String? = null,
    val trackIds: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

 data class LibraryArtist(
    val id: String = "",
    val name: String = "",
    val sortName: String = "",
    val createdAt: String = "",
    val artworkPath: String? = null,
)

 data class LibraryAlbum(
    val id: String = "",
    val title: String = "",
    val sortTitle: String = "",
    val sortArtist: String = "",
    val artist: String = "",
    val artworkPath: String? = null,
    val createdAt: String = "",
    val year: Int = 0,
    val copyright: String = "",
)

 data class LibraryGenre(
    val id: String = "",
    val name: String = "",
    val trackCount: Int = 0,
    val sortName: String = "",
    val createdAt: String = "",
)

 data class LibraryComposer(
    val id: String = "",
    val name: String = "",
    val sortName: String = "",
    val createdAt: String = "",
    val artworkPath: String? = null,
)

 data class LibrarySearchCandidate(
    val entityType: String,
    val entityId: String,
    val title: String,
    val subtitle: String = "",
)

 data class SyncTrackEntity(
    val planId: String,
    val trackId: String,
    val title: String,
    val artists: String,
    val album: String = "",
    val albumId: String = "",
    val artworkKey: String? = null,
    val playCount: Int = 0,
    val createdAt: String = "",
    val discNumber: Int = 0,
    val trackNumber: Int = 0,
    val syncOrder: Int = 0,
    val rawJson: String = "{}",
)

 data class SyncAssetEntity(
    val planId: String,
    val assetId: String,
    val kind: String,
    val sha256: String = "",
    val size: Long = 0L,
    val relativePath: String = "",
)

 data class SyncPlaylistEntity(
    val planId: String,
    val playlistId: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

 data class LibrarySearchDocumentEntity(
    val planId: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val subtitle: String = "",
    val keywords: List<String> = emptyList(),
)

 data class SyncPlanEntity(
    val planId: String,
    val source: String,
    val manifestJson: String,
    val status: String,
    val isActive: Boolean = false,
)

/** ============================================================================
 *  JSON helpers
 * ============================================================================ */

 object LibrarySyncProtocol {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}

/** ============================================================================
 *  Extension: LibraryTrack.metadataObject()
 * ============================================================================ */

 fun LibraryTrack.metadataObject(): JsonObject? =
    try {
        LibrarySyncProtocol.json.parseToJsonElement(metadataJson) as? JsonObject
    } catch (e: Exception) {
        null
    }

/** ============================================================================
 *  Display helpers
 * ============================================================================ */

 fun trackDisplayTitle(track: LibraryTrack): String =
    track.title.ifBlank { track.id.ifBlank { "Unknown song" } }

 fun trackDisplayArtists(track: LibraryTrack): String =
    track.artists.ifBlank { "Unknown artist" }

/** ============================================================================
 *  Mapper helpers (recreated for sync mapper tests)
 * ============================================================================ */

 fun libraryArtistsFrom(
    tracks: List<LibraryTrackRow>,
    artworkPaths: Map<String, String> = emptyMap(),
): List<LibraryArtist> {
    val seen = mutableMapOf<String, LibraryArtist>()
    val trackArtwork = tracks.associateBy({ it.id }, { it.artworkPath })
    for (track in tracks) {
        val rawJson = track.rawJson
        val root = runCatching { LibrarySyncProtocol.json.parseToJsonElement(rawJson) as? JsonObject }.getOrNull()
            ?: continue
        val artistsArr = root.get("artists") as? JsonArray ?: continue
        for (entry in artistsArr) {
            val obj = entry as? JsonObject ?: continue
            val id = obj.get("id")?.jsonPrimitive?.contentOrNull ?: continue
            if (id.isBlank()) continue
            val name = obj.get("name")?.jsonPrimitive?.contentOrNull ?: continue
            if (name.isBlank()) continue
            val explicitSortName = obj.get("sort_name")?.jsonPrimitive?.contentOrNull
                ?: obj.get("normalization_key")?.jsonPrimitive?.contentOrNull
            val explicitCreatedAt = obj.get("created_at")?.jsonPrimitive?.contentOrNull
            val artworkKey = obj.get("artwork_key")?.jsonPrimitive?.contentOrNull
                ?: obj.get("artwork_key_local")?.jsonPrimitive?.contentOrNull
            var artworkPath: String? = null
            if (artworkKey != null) {
                artworkPath = artworkPaths[artworkKey] ?: artworkPaths["artwork:$id"] ?: artworkPaths[artworkKey.removePrefix("artwork:")]
                if (artworkPath == null) {
                    val stagedKey = artworkKey.removePrefix("artwork:")
                    artworkPath = artworkPaths[stagedKey]
                }
            }
            if (artworkPath == null) {
                artworkPath = trackArtwork[track.id]
            }
            val existing = seen[id]
            seen[id] = LibraryArtist(
                id = id,
                name = name,
                sortName = explicitSortName ?: existing?.sortName ?: name,
                createdAt = explicitCreatedAt ?: existing?.createdAt ?: track.createdAt,
                artworkPath = artworkPath ?: existing?.artworkPath,
            )
        }
    }
    return seen.values.toList()
}

 fun libraryAlbumsFrom(
    tracks: List<LibraryTrackRow>,
    artworkPaths: Map<String, String> = emptyMap(),
): List<LibraryAlbum> {
    val seen = mutableMapOf<String, LibraryAlbum>()
    val trackArtwork = tracks.associateBy({ it.id }, { it.artworkPath })
    for (track in tracks) {
        val root = runCatching { LibrarySyncProtocol.json.parseToJsonElement(track.rawJson) as? JsonObject }.getOrNull()
            ?: continue
        val albumObj = root.get("album") as? JsonObject ?: continue
        val albumId = albumObj.get("id")?.jsonPrimitive?.contentOrNull ?: continue
        if (albumId.isBlank()) continue
        val title = albumObj.get("title")?.jsonPrimitive?.contentOrNull ?: ""
        if (title.isBlank()) continue
        val sortTitle = albumObj.get("sort_title")?.jsonPrimitive?.contentOrNull ?: title
        val year = albumObj.get("year")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val copyright = albumObj.get("copyright")?.jsonPrimitive?.contentOrNull ?: ""
        val createdAt = albumObj.get("created_at")?.jsonPrimitive?.contentOrNull ?: track.createdAt
        var artworkKey = albumObj.get("artwork_key")?.jsonPrimitive?.contentOrNull
        var artworkPath: String? = null
        if (artworkKey != null) {
            artworkPath = artworkPaths[artworkKey] ?: artworkPaths["artwork:$albumId"]
            if (artworkPath == null) {
                val stagedKey = artworkKey.removePrefix("artwork:")
                artworkPath = artworkPaths[stagedKey]
            }
        }
        if (artworkPath == null) {
            artworkPath = trackArtwork[track.id]
        }
        // Resolve album artist
        var artist = ""
        var sortArtist = ""
        var explicitSortArtist = false
        val albumArtists = root.get("album_artists") as? JsonArray
        if (albumArtists != null && albumArtists.isNotEmpty()) {
            val first = albumArtists.firstOrNull { it is JsonObject } as? JsonObject
            artist = first?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
            val explicitSortName = first?.get("sort_name")?.jsonPrimitive?.contentOrNull
            sortArtist = explicitSortName ?: artist
            explicitSortArtist = explicitSortName != null
        }
        if (artist.isBlank()) {
            val trackArtists = root.get("artists") as? JsonArray
            val firstTrackArtist = trackArtists?.firstOrNull { it is JsonObject } as? JsonObject
            artist = firstTrackArtist?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
            val explicitSortName = firstTrackArtist?.get("sort_name")?.jsonPrimitive?.contentOrNull
            sortArtist = explicitSortName ?: artist
            explicitSortArtist = explicitSortName != null
        }
        val existing = seen[albumId]
        seen[albumId] = LibraryAlbum(
            id = albumId,
            title = title,
            sortTitle = sortTitle,
            sortArtist = if (explicitSortArtist) sortArtist else existing?.sortArtist ?: sortArtist,
            artist = if (artist.isNotBlank()) artist else existing?.artist ?: artist,
            artworkPath = artworkPath ?: existing?.artworkPath,
            createdAt = createdAt,
            year = year,
            copyright = copyright,
        )
    }
    return seen.values.toList()
}

 fun libraryComposersFrom(
    tracks: List<LibraryTrackRow>,
    artworkPaths: Map<String, String> = emptyMap(),
): List<LibraryComposer> {
    val seen = mutableMapOf<String, LibraryComposer>()
    val trackArtwork = tracks.associateBy({ it.id }, { it.artworkPath })
    for (track in tracks) {
        val root = runCatching { LibrarySyncProtocol.json.parseToJsonElement(track.rawJson) as? JsonObject }.getOrNull()
            ?: continue
        val composersArr = root.get("composers") as? JsonArray ?: continue
        for (entry in composersArr) {
            val obj = entry as? JsonObject ?: continue
            val id = obj.get("id")?.jsonPrimitive?.contentOrNull ?: continue
            if (id.isBlank()) continue
            val name = obj.get("name")?.jsonPrimitive?.contentOrNull ?: continue
            if (name.isBlank()) continue
            val explicitSortName = obj.get("sort_name")?.jsonPrimitive?.contentOrNull
                ?: obj.get("normalization_key")?.jsonPrimitive?.contentOrNull
            val explicitCreatedAt = obj.get("created_at")?.jsonPrimitive?.contentOrNull
            val artworkKey = obj.get("artwork_key")?.jsonPrimitive?.contentOrNull
            var artworkPath: String? = null
            if (artworkKey != null) {
                artworkPath = artworkPaths[artworkKey] ?: artworkPaths["artwork:$id"]
                if (artworkPath == null) {
                    val stagedKey = artworkKey.removePrefix("artwork:")
                    artworkPath = artworkPaths[stagedKey]
                }
            }
            if (artworkPath == null) {
                artworkPath = trackArtwork[track.id]
            }
            val existing = seen[id]
            seen[id] = LibraryComposer(
                id = id,
                name = name,
                sortName = explicitSortName ?: existing?.sortName ?: name,
                createdAt = explicitCreatedAt ?: existing?.createdAt ?: track.createdAt,
                artworkPath = artworkPath ?: existing?.artworkPath,
            )
        }
    }
    return seen.values.toList()
}

fun libraryGenresFrom(
    tracks: List<LibraryTrackRow>,
): List<LibraryGenre> {
    val seen = mutableMapOf<String, LibraryGenre>()
    for (track in tracks) {
        val root = runCatching { LibrarySyncProtocol.json.parseToJsonElement(track.rawJson) as? JsonObject }.getOrNull()
            ?: continue
        val genresArr = root.get("genres") as? JsonArray ?: continue
        for (entry in genresArr) {
            val obj = entry as? JsonObject ?: continue
            val id = obj.get("id")?.jsonPrimitive?.contentOrNull ?: continue
            if (id.isBlank()) continue
            val name = obj.get("name")?.jsonPrimitive?.contentOrNull ?: continue
            if (name.isBlank()) continue
            val explicitSortName = obj.get("sort_name")?.jsonPrimitive?.contentOrNull
                ?: obj.get("normalization_key")?.jsonPrimitive?.contentOrNull
            val explicitCreatedAt = obj.get("created_at")?.jsonPrimitive?.contentOrNull
            val existing = seen[id]
            seen[id] = LibraryGenre(
                id = id,
                name = name,
                sortName = explicitSortName ?: existing?.sortName ?: name,
                createdAt = explicitCreatedAt ?: existing?.createdAt ?: track.createdAt,
            )
        }
    }
    return seen.values.toList()
}

/** ============================================================================
 *  FTS search match builder
 * ============================================================================ */

 fun searchFtsMatch(query: String): String? {
    val normalized = java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    val tokens = normalized
        .lowercase()
        .split(Regex("[\\s/]+"))
        .filter { it.isNotBlank() }
        .map { it.replace(Regex("[^a-z0-9]"), "") }
        .filter { it.isNotBlank() }
    return if (tokens.isEmpty()) null else tokens.joinToString(" AND ") { "$it*" }
}

/** ============================================================================
 *  Search document builder
 * ============================================================================ */

 fun searchDocumentsFor(
    planId: String,
    entities: List<SyncTrackEntity>,
    playlists: List<SyncPlaylistEntity>,
): List<LibrarySearchDocumentEntity> {
    val docs = mutableListOf<LibrarySearchDocumentEntity>()
    for (entity in entities) {
        val root = runCatching { LibrarySyncProtocol.json.parseToJsonElement(entity.rawJson) as? JsonObject }.getOrNull()
        if (root == null) continue
        val title = root.get("title")?.jsonPrimitive?.contentOrNull.orEmpty()
        val artists = root.get("artists") as? JsonArray
        val artistNames = artists?.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull }.orEmpty()
        val subtitle = artistNames.joinToString(", ")
        val keywords = mutableListOf(title)
        keywords.addAll(artistNames)
        val albumObj = root.get("album") as? JsonObject
        val albumTitle = albumObj?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty()
        if (albumTitle.isNotBlank()) keywords.add(albumTitle)
        docs.add(
            LibrarySearchDocumentEntity(
                planId = planId,
                entityType = "track",
                entityId = entity.trackId,
                title = title,
                subtitle = subtitle,
                keywords = keywords.distinct(),
            ),
        )
        // Composer documents
        val composersArr = root.get("composers") as? JsonArray
        if (composersArr != null) {
            for (entry in composersArr) {
                val obj = entry as? JsonObject ?: continue
                val composerId = obj.get("id")?.jsonPrimitive?.contentOrNull ?: continue
                if (composerId.isBlank()) continue
                val composerName = obj.get("name")?.jsonPrimitive?.contentOrNull ?: continue
                if (composerName.isBlank()) continue
                docs.add(
                    LibrarySearchDocumentEntity(
                        planId = planId,
                        entityType = "composer",
                        entityId = composerId,
                        title = composerName,
                        keywords = listOf(composerName),
                    ),
                )
            }
        }
    }
    for (playlist in playlists) {
        docs.add(
            LibrarySearchDocumentEntity(
                planId = planId,
                entityType = "playlist",
                entityId = playlist.playlistId,
                title = playlist.name,
                keywords = listOf(playlist.name),
            ),
        )
    }
    return docs
}

/** ============================================================================
 *  Artist artwork mutation types
 * ============================================================================ */

 enum class ArtistArtworkOperation { SET_ARTWORK, REMOVE_ARTWORK }

 data class ArtistArtworkPayload(val sha256: String?)

 data class ArtistMutation(
    val mutationId: String,
    val artistId: String,
    val operation: ArtistArtworkOperation,
    val updatedAt: Long,
    val payload: ArtistArtworkPayload? = null,
) {
    fun validationError(): String? =
        when {
            operation == ArtistArtworkOperation.SET_ARTWORK && (payload?.sha256.isNullOrBlank() == true) ->
                "SET_ARTWORK requires a valid sha256"
            else -> null
        }
}

/** ============================================================================
 *  Staged artwork types (used by ArtistArtworkFiles / PlaylistArtworkFiles)
 * ============================================================================ */

 data class StagedArtistArtwork(
    val sha256: String,
    val mime: String,
    val sizeBytes: Long,
    val relativePath: String,
)

 data class StagedPlaylistArtwork(
    val sha256: String,
    val mime: String,
    val sizeBytes: Long,
    val relativePath: String,
)

/** ============================================================================
 *  Playlist mutation types
 * ============================================================================ */

 enum class PlaylistMutationOperation {
    CREATE, ADD_TRACK, REMOVE_TRACK, UPDATE, SET_ARTWORK, REMOVE_ARTWORK, DELETE, MOVE_TRACK
}

 data class PlaylistMutationPayload(
    val name: String? = null,
    val trackId: String? = null,
    val artworkSha256: String? = null,
    val previousTrackId: String? = null,
    val nextTrackId: String? = null,
)

 data class PlaylistMutation(
    val mutationId: String,
    val playlistId: String,
    val operation: PlaylistMutationOperation,
    val updatedAt: Long,
    val payload: PlaylistMutationPayload = PlaylistMutationPayload(),
)

/** ============================================================================
 *  applyPendingArtistArtworkMutations
 * ============================================================================ */

 fun applyPendingArtistArtworkMutations(
    existing: Map<String, String>,
    pending: List<ArtistMutation>,
): Map<String, String> {
    var result = existing.toMutableMap()
    for (mutation in pending) {
        when (mutation.operation) {
            ArtistArtworkOperation.SET_ARTWORK -> {
                mutation.payload?.sha256?.let { result[mutation.artistId] = it }
            }
            ArtistArtworkOperation.REMOVE_ARTWORK -> {
                result.remove(mutation.artistId)
            }
        }
    }
    return result
}

/** ============================================================================
 *  Stable ID helpers
 * ============================================================================ */

 fun stableId(prefix: String, name: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(name.lowercase().toByteArray())
    return prefix + digest.joinToString("") { "%02x".format(it) }.take(12)
}

 fun artistIdFor(name: String): String = stableId("local-artist-", name)
 fun albumIdFor(name: String): String = stableId("local-album-", name)
 fun composerIdFor(name: String): String = stableId("local-composer-", name)
 fun genreIdFor(name: String): String = stableId("local-genre-", name)

/** ============================================================================
 *  AndroidLibrarySyncStore — local-only replacement for the Room-backed store
 * ============================================================================ */

 class AndroidLibrarySyncStore(private val context: Context) {

    private val _tracks = MutableStateFlow(emptyList<LibraryTrack>())
    val tracks: StateFlow<List<LibraryTrack>> = _tracks.asStateFlow()

    private val _playlists = MutableStateFlow(emptyList<LibraryPlaylist>())
    val playlists: StateFlow<List<LibraryPlaylist>> = _playlists.asStateFlow()

    private val _albums = MutableStateFlow(emptyList<LibraryAlbum>())
    val albums: StateFlow<List<LibraryAlbum>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow(emptyList<LibraryArtist>())
    val artists: StateFlow<List<LibraryArtist>> = _artists.asStateFlow()

    private val _genres = MutableStateFlow(emptyList<LibraryGenre>())
    val genres: StateFlow<List<LibraryGenre>> = _genres.asStateFlow()

    private val _composers = MutableStateFlow(emptyList<LibraryComposer>())
    val composers: StateFlow<List<LibraryComposer>> = _composers.asStateFlow()

    private val _moodRadioEligibleTrackIds = MutableStateFlow(emptySet<String>())
    val moodRadioEligibleTrackIds: StateFlow<Set<String>> = _moodRadioEligibleTrackIds.asStateFlow()

    private val _analysisAvailable = MutableStateFlow(false)
    val analysisAvailable: StateFlow<Boolean> = _analysisAvailable.asStateFlow()

    private val _libraryAnalysisEnabled = MutableStateFlow(false)
    val libraryAnalysisEnabled: StateFlow<Boolean> = _libraryAnalysisEnabled.asStateFlow()

    private val _lastListenedAt = MutableStateFlow(emptyMap<String, Long>())
    val lastListenedAt: StateFlow<Map<String, Long>> = _lastListenedAt.asStateFlow()

    private val _artistArtworkKeys = MutableStateFlow(emptyMap<String, String>())
    val artistArtworkKeys: StateFlow<Map<String, String>> = _artistArtworkKeys.asStateFlow()

    private val _artworkPaths = MutableStateFlow(emptyMap<String, String>())
    val artworkPaths: StateFlow<Map<String, String>> = _artworkPaths.asStateFlow()

    private val _dailyTrackListeningStats = MutableStateFlow(emptyList<DailyTrackListeningStat>())
    val dailyTrackListeningStats: StateFlow<List<DailyTrackListeningStat>> = _dailyTrackListeningStats.asStateFlow()

    private val _dailyPlaybackAttemptStats = MutableStateFlow(emptyList<DailyPlaybackAttemptStat>())
    val dailyPlaybackAttemptStats: StateFlow<List<DailyPlaybackAttemptStat>> = _dailyPlaybackAttemptStats.asStateFlow()

    private val analyses = mutableMapOf<String, TrackAnalysis>()
    val activeAnalyses: Map<String, TrackAnalysis> get() = analyses.toMap()

    private val providerLyricsStore = mutableMapOf<String, String>()
    private val _providerLyrics = mutableMapOf<String, MutableStateFlow<String?>>()
    fun providerLyrics(trackId: String): Flow<String?> =
        _providerLyrics.getOrPut(trackId) {
            MutableStateFlow(providerLyricsStore[trackId])
        }

    private val favorites = mutableSetOf<String>()
    fun isFavorite(trackId: String): Boolean = favorites.contains(trackId)

    /** Commit a local import, replacing all tracks for the plan. */
    suspend fun commitLocalLibrary(import: LocalImport) {
        val libraryTracks = import.tracks.map { entity ->
            LibraryTrack(
                id = entity.trackId,
                title = entity.title,
                artists = entity.artists,
                album = entity.album,
                albumId = entity.albumId,
                artworkKey = entity.artworkKey,
                playCount = entity.playCount,
                createdAt = entity.createdAt,
                updatedAt = entity.createdAt,
                discNumber = entity.discNumber,
                trackNumber = entity.trackNumber,
                syncOrder = entity.syncOrder,
                metadataJson = entity.rawJson,
                artworkPath = null,
                audioPath = import.audioAssets.firstOrNull { it.assetId == "audio:${entity.trackId}" }?.relativePath,
                sortTitle = entity.title,
                sortArtists = entity.artists,
            )
        }
        _tracks.value = libraryTracks
        _analysisAvailable.value = libraryTracks.isNotEmpty()
        _moodRadioEligibleTrackIds.value = libraryTracks.map { it.id }.toSet()
    }

    /** Return playlist entities for a plan (stubbed — returns empty). */
    fun playlistEntities(planId: String): List<SyncPlaylistEntity> = emptyList()

    /** Find a track by ID. */
    fun trackById(trackId: String): LibraryTrack? = tracks.value.firstOrNull { it.id == trackId }

    /** Tracks eligible for mood radio. */
    fun moodRadioTracks(): List<MoodRadioTrack> =
        tracks.value
            .filter { it.id in moodRadioEligibleTrackIds.value }
            .map { MoodRadioTrack(id = it.id, albumId = it.albumId, primaryArtistId = it.artists, energy = null, danceability = null, brightness = null, tempo = null) }

    /** Set favorite state on a track. */
    suspend fun setFavorite(trackId: String, favorite: Boolean) {
        if (favorite) favorites.add(trackId) else favorites.remove(trackId)
    }

    /** Save provider lyrics for a track. */
    suspend fun saveProviderLyrics(trackId: String, content: String, source: String) {
        providerLyricsStore[trackId] = content
        _providerLyrics.getOrPut(trackId) { MutableStateFlow(content) }.value = content
    }

    /** Get analysis for a track (stubbed — returns null). */
    fun analysis(trackId: String): TrackAnalysis? = analyses[trackId]

    /** Record a listening write (stubbed — no-op). */
    suspend fun recordListening(write: Any) {}

    /** Recover open playback attempts (stubbed — no-op). */
    suspend fun recoverOpenPlaybackAttempts(nowMs: Long) {}

    /** Cleanup old listening records (stubbed — no-op). */
    suspend fun cleanupListening(cutoffMs: Long) {}

    /** Stage artist artwork (stubbed — no-op). */
    suspend fun stageArtistArtwork(artwork: StagedArtistArtwork) {}

    /** Queue an artist artwork mutation (stubbed — no-op). */
    suspend fun queueArtistArtworkMutation(mutation: ArtistMutation) {}

    /** Create a local playlist (stubbed — no-op). */
    suspend fun createLocalPlaylist(
        mutation: PlaylistMutation,
        artwork: StagedPlaylistArtwork?,
        artworkMutationId: String?,
        initialTrackIds: List<String>,
    ) {}

    /** Queue a playlist mutation (stubbed — no-op). */
    suspend fun queuePlaylistMutation(mutation: PlaylistMutation) {}

    /** Discard a failed local playlist (stubbed — returns false). */
    fun discardFailedLocalPlaylist(playlistId: String): Boolean = false

    /** Stage playlist artwork (stubbed — no-op). */
    suspend fun stagePlaylistArtwork(artwork: StagedPlaylistArtwork) {}

    /** Search candidates (stubbed — returns empty). */
    suspend fun searchCandidates(text: String): List<LibrarySearchCandidate> = emptyList()
}

/** ============================================================================
 *  AndroidSyncRuntime replacement — object with same API as deleted SyncRuntime.kt
 * ============================================================================ */

 object AndroidSyncRuntime {
    private var currentStore: AndroidLibrarySyncStore? = null

    fun initialize(context: Context) {
        if (currentStore == null) {
            currentStore = AndroidLibrarySyncStore(context.applicationContext)
        }
    }

    val syncStore: AndroidLibrarySyncStore
        get() = currentStore
            ?: error("AndroidLibrarySyncStore not initialized. Call AndroidSyncRuntime.initialize(context) first.")
}

/** Stub: splits listening time across local dates. */
fun splitListeningByDate(startMs: Long, endMs: Long, intervalMs: Long): Map<String, Long> {
    val zone = java.time.ZoneId.of("UTC")
    val startDate = java.time.Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate().toString()
    val endDate = java.time.Instant.ofEpochMilli(endMs).atZone(zone).toLocalDate().toString()
    val totalSeconds = ((endMs - startMs) / 1000).coerceAtLeast(0)
    val perDate = totalSeconds / 2
    return mapOf(startDate to perDate, endDate to perDate)
}

/** Stub: returns cached asset path if the file exists and path is safe. */
fun cachedAssetPath(filesDir: java.io.File, entity: SyncAssetEntity): String? {
    val file = java.io.File(filesDir, entity.relativePath)
    return if (file.exists() && !entity.relativePath.contains("..")) entity.relativePath else null
}
