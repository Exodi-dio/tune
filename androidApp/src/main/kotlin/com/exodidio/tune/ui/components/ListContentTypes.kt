package com.exodidio.tune.ui.components

// TDD scaffold pinning the stable contentType strings used by Task 10 list edits.
// The list sites use inline literals per brief; this object mirrors them so JVM tests pin stability.
internal object ListContentTypes {
    const val HOME_TRACK = "home_track"
    const val SEARCH_GRID_PREFIX = "search_grid_"
    const val SEARCH_ROW_PREFIX = "search_row_"
    const val ALBUM_TRACK = "album_track"
    const val PLAYLIST_TRACK = "playlist_track"
    const val TRACK_INFO_ROW = "track_info_row"
    const val PLAYLIST_PICKER_ROW = "playlist_picker_row"
    const val LYRICS_RESULT = "lyrics_result"
    const val INSIGHT_ARTIST = "insight_artist"
}
