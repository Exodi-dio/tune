package com.exodidio.tune.player

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.metadataObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val MaxOfflineAutoplay: Int = 10

fun shouldSuppressAutoplay(repeatMode: RepeatMode): Boolean =
    repeatMode == RepeatMode.One

fun shouldRefillAutoplay(upcomingCount: Int, repeatMode: RepeatMode, autoplayEnabled: Boolean): Boolean {
    if (!autoplayEnabled) return false
    if (shouldSuppressAutoplay(repeatMode)) return false
    return upcomingCount <= 1
}

fun nextAutoplayTriggerIndex(activeTrackIds: List<String>, currentIndex: Int): Int =
    (activeTrackIds.size - currentIndex - 1).coerceAtLeast(0)

fun pickAutoplay(
    library: List<LibraryTrack>,
    currentTrack: LibraryTrack,
    recentIds: Set<String>,
    limit: Int = 10
): List<String> {
    if (limit <= 0) return emptyList()
    val cap = limit.coerceAtMost(MaxOfflineAutoplay)
    val excluded = recentIds + currentTrack.id
    val candidates = library.filter { it.id.isNotBlank() && it.id !in excluded }
    if (candidates.isEmpty()) return emptyList()
    val currentArtists = artistTokens(currentTrack.artists)
    val currentGenres = genreTokens(currentTrack)
    val sameArtist = candidates.filter { artistTokens(it.artists).any(currentArtists::contains) }
    val sameGenre = candidates.filter {
        it.id !in sameArtist.map(LibraryTrack::id) &&
            genreTokens(it).any(currentGenres::contains)
    }
    val picked = mutableListOf<LibraryTrack>()
    val pickedIds = mutableSetOf<String>()
    fun takeFrom(source: List<LibraryTrack>) {
        val ordered = source.sortedWith(compareByDescending<LibraryTrack> { it.playCount }.thenBy { it.title })
        for (track in ordered) {
            if (picked.size >= cap) return
            if (pickedIds.add(track.id)) picked += track
        }
    }
    takeFrom(sameArtist.sortedWith(compareByDescending<LibraryTrack> { it.playCount }.thenBy { it.title }))
    if (picked.size < cap) takeFrom(sameGenre)
    if (picked.size < cap) {
        val remainder = candidates.filter { it.id !in pickedIds }
            .sortedWith(compareBy<LibraryTrack> { if (it.albumId == currentTrack.albumId) 0 else 1 }.thenByDescending { it.playCount }.thenBy { it.title })
        takeFrom(remainder)
    }
    return picked.take(cap).map { it.id }
}

private fun artistTokens(artists: String): Set<String> =
    artists.split(",", ";", "&").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

private fun genreTokens(track: LibraryTrack): Set<String> {
    val obj: JsonObject = track.metadataObject() ?: return emptySet()
    val names = mutableSetOf<String>()
    (obj["genres"] as? JsonArray)?.forEach { (it as? JsonObject)?.get("name")?.jsonPrimitive?.content?.let(names::add) }
    (obj["genre"] as? JsonObject)?.get("name")?.jsonPrimitive?.content?.let(names::add)
    obj["raw_genre_names"]?.let { (it as? JsonArray)?.forEach { v -> v.jsonPrimitive.content.let(names::add) } }
    return names.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
}
