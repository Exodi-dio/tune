package com.exodidio.tune.ui.navigation

import com.exodidio.tune.R
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.components.MaterialSymbols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Fix wave (C2): pins the now-playing quality badge mapping (mirror of the old
// FullScreenPlayerControls behavior) using trackAudioQuality/trackInfoValues.
class PlayerShellQualityBadgeTest {
    private fun track(metadataJson: String): LibraryTrack =
        LibraryTrack(id = "t1", title = "Title", artists = "Artist", metadataJson = metadataJson)

    @Test
    fun losslessHiResAndDsdRenderABadge() {
        assertEquals(
            R.string.track_info_quality_lossless to MaterialSymbols.GraphicEq,
            shellQualityBadge(track("{\"format\":\"flac\",\"bit_depth\":16,\"sample_rate\":44100}")),
        )
        assertEquals(
            R.string.track_info_quality_hi_res to MaterialSymbols.Bolt,
            shellQualityBadge(track("{\"format\":\"flac\",\"bit_depth\":24,\"sample_rate\":96000}")),
        )
        assertEquals(
            R.string.track_info_quality_dsd to MaterialSymbols.Crown,
            shellQualityBadge(track("{\"format\":\"dsf\"}")),
        )
    }

    @Test
    fun lossyUnknownAndMissingTracksStayHidden() {
        assertNull(shellQualityBadge(track("{\"format\":\"mp3\"}")))
        assertNull(shellQualityBadge(track("{}")))
        assertNull(shellQualityBadge(null))
    }

    @Test
    fun dialogDetailsKeepSampleRateBitDepthAndCodecOnly() {
        val details = shellQualityDetails(
            track("{\"format\":\"flac\",\"bit_depth\":16,\"sample_rate\":44100,\"codec\":\"flac\"}"),
        )
        assertEquals(
            listOf(R.string.track_info_sample_rate, R.string.track_info_bit_depth, R.string.track_info_codec),
            details.map { it.labelRes },
        )
        assertEquals("44.1 kHz", details.first { it.labelRes == R.string.track_info_sample_rate }.value)
        assertEquals("16-bit", details.first { it.labelRes == R.string.track_info_bit_depth }.value)
    }
}
