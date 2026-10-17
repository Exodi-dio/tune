package com.exodidio.tune.mood

/** A track eligible for mood radio, with optional audio analysis features. */
data class MoodRadioTrack(
    val id: String,
    val albumId: String,
    val primaryArtistId: String,
    val energy: Double? = null,
    val danceability: Double? = null,
    val brightness: Double? = null,
    val tempo: Double? = null,
)
