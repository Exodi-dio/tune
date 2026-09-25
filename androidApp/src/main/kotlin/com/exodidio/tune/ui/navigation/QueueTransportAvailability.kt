package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.PlaybackQueueSnapshot

/** Transport affordance policy for the active queue order. */
internal fun PlaybackQueueSnapshot.canNavigatePrevious(): Boolean =
    activeTrackIds.isEmpty() || currentTrackId != null

internal fun PlaybackQueueSnapshot.canNavigateNext(): Boolean =
    activeTrackIds.isEmpty() || currentTrackId != null

internal fun canDispatchQueueSwipe(
    swipeDirection: Int,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
): Boolean = when {
    swipeDirection < 0 -> canNavigateNext
    swipeDirection > 0 -> canNavigatePrevious
    else -> false
}
