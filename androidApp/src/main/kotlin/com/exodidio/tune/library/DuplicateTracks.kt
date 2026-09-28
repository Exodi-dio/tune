package com.exodidio.tune.library

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.metadataObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

/**
 * Duplicates grouping engine.
 *
 * Two tracks are duplicates when their normalized title, normalized artist, and
 * durations agree. Normalization is trim + lowercase, mirroring the
 * trim/case-folding conventions of the library search helpers
 * (normalizedLibrarySearchText in ui.screens).
 *
 * Duration tolerance is ±[DUPLICATE_DURATION_TOLERANCE_SEC] seconds, read from the
 * metadata `duration` **seconds** field.
 *
 * Tracks with no parseable duration are EXCLUDED from all groups: without a
 * duration, common title+artist pairs (e.g. original vs. live vs. remaster) would
 * produce false positives, and a missing value must not silently count as zero.
 */
const val DUPLICATE_DURATION_TOLERANCE_SEC = 2

/** One set of duplicate tracks sharing a normalized title+artist key. */
data class DuplicateGroup(
    val key: String,
    val trackIds: List<String>,
)

internal fun normalizeDuplicateText(value: String): String = value.trim().lowercase()

/**
 * Duration in whole seconds from metadata `duration`, or null when
 * absent/unparseable. Accepts float strings (e.g. "200.5", rounded to the
 * nearest second) because scanners may emit fractional durations.
 */
fun duplicateDurationSec(track: LibraryTrack): Int? =
    track.metadataObject()?.get("duration")?.jsonPrimitive?.contentOrNull
        ?.trim()?.toDoubleOrNull()?.roundToInt()

private fun metadataInt(track: LibraryTrack, vararg keys: String): Int? {
    val metadata = track.metadataObject() ?: return null
    return keys.firstNotNullOfOrNull { key ->
        metadata[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }
}

private fun metadataLong(track: LibraryTrack, vararg keys: String): Long? {
    val metadata = track.metadataObject() ?: return null
    return keys.firstNotNullOfOrNull { key ->
        metadata[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
    }
}

private fun metadataString(track: LibraryTrack, vararg keys: String): String? {
    val metadata = track.metadataObject() ?: return null
    return keys.firstNotNullOfOrNull { key ->
        metadata[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }
}

private val LOSSLESS_CODECS = setOf("flac", "alac", "wav", "wave", "aiff", "ape", "wv", "wvp")

/**
 * Quality ranking for [chooseKeepBest]. Documented ordering (no shared
 * `trackAudioQuality` helper exists in JVM-accessible code, so the ordering lives
 * here): lossless first, then higher bitrate, then higher bit depth, then higher
 * sample rate, then larger file size. Size is read from metadata
 * (`size`/`file_size`/`filesize`, bytes) because [LibraryTrack] itself carries no
 * file-size field.
 */
internal data class DuplicateQuality(
    val lossless: Boolean,
    val bitrate: Int,
    val bitDepth: Int,
    val sampleRateHz: Int,
    val sizeBytes: Long,
)

internal fun duplicateQualityOf(track: LibraryTrack): DuplicateQuality {
    val explicitLossless = track.metadataObject()?.get("lossless")?.jsonPrimitive?.contentOrNull
        ?.lowercase() == "true"
    val codec = (
        metadataString(track, "codec", "audio_codec", "format")
            ?: ((track.metadataObject()?.get("codecs") as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull)
        ).orEmpty().lowercase()
    return DuplicateQuality(
        lossless = explicitLossless || codec in LOSSLESS_CODECS,
        bitrate = metadataInt(track, "bitrate") ?: 0,
        bitDepth = metadataInt(track, "bit_depth", "bitDepth", "bits_per_sample") ?: 0,
        sampleRateHz = metadataInt(track, "sample_rate", "sampleRate", "sample_rate_hz") ?: 0,
        sizeBytes = metadataLong(track, "size", "file_size", "filesize") ?: 0L,
    )
}

/**
 * Groups [tracks] into duplicate sets. Only groups of two or more tracks are
 * returned; duration-less tracks never appear in any group. Within one
 * title+artist bucket, durations are clustered greedily on sorted durations: a
 * track joins the current cluster while it is within ±[DUPLICATE_DURATION_TOLERANCE_SEC]
 * seconds of the cluster's first (shortest) duration, otherwise it starts a new
 * cluster.
 */
fun groupDuplicateTracks(tracks: List<LibraryTrack>): List<DuplicateGroup> {
    data class Keyed(val track: LibraryTrack, val title: String, val artist: String, val duration: Int)

    val keyed = tracks.mapNotNull { track ->
        val duration = duplicateDurationSec(track) ?: return@mapNotNull null
        Keyed(track, normalizeDuplicateText(track.title), normalizeDuplicateText(track.artists), duration)
    }
    return keyed
        .groupBy { it.title to it.artist }
        .toSortedMap(compareBy({ it.first }, { it.second }))
        .flatMap { (bucketKey, bucket) ->
            val sorted = bucket.sortedWith(compareBy({ it.duration }, { it.track.id }))
            val clusters = mutableListOf<MutableList<Keyed>>()
            for (entry in sorted) {
                val current = clusters.lastOrNull()
                if (current != null && entry.duration - current.first().duration <= DUPLICATE_DURATION_TOLERANCE_SEC) {
                    current += entry
                } else {
                    clusters += mutableListOf(entry)
                }
            }
            clusters.filter { it.size >= 2 }.map { cluster ->
                DuplicateGroup(
                    key = "${bucketKey.first} | ${bucketKey.second}",
                    trackIds = cluster.map { it.track.id },
                )
            }
        }
}

/**
 * Picks the track id to keep from [group]: highest [DuplicateQuality], ties broken
 * by lexicographically smallest id for determinism. Returns null when none of the
 * group's ids resolve in [tracksById].
 */
fun chooseKeepBest(group: DuplicateGroup, tracksById: Map<String, LibraryTrack>): String? =
    group.trackIds.mapNotNull { id -> tracksById[id]?.let { id to duplicateQualityOf(it) } }
        .sortedWith(
            compareByDescending<Pair<String, DuplicateQuality>> { it.second.lossless }
                .thenByDescending { it.second.bitrate }
                .thenByDescending { it.second.bitDepth }
                .thenByDescending { it.second.sampleRateHz }
                .thenByDescending { it.second.sizeBytes }
                .thenBy { it.first },
        )
        .firstOrNull()?.first

/**
 * Ids to remove from the LIBRARY ONLY when keeping the best copy of [group]:
 * every grouped id except the [chooseKeepBest] winner. Pure computation over
 * ids — it never touches files, the store, or its inputs. Unknown groups yield
 * an empty list (nothing to remove).
 */
fun keepBestRemovalIds(group: DuplicateGroup, tracksById: Map<String, LibraryTrack>): List<String> {
    val keep = chooseKeepBest(group, tracksById) ?: return emptyList()
    return group.trackIds.filter { it != keep }
}
