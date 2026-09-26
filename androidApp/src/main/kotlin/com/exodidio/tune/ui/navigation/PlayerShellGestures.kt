package com.exodidio.tune.ui.navigation

import kotlin.math.abs

// Adapted from BitChord NowPlayingScreen (GPL-3.0): swipeThreshold 72dp,
// QUEUE_TRAVEL_MS 420, QUEUE_CARRY_FRACTION 0.3f, QUEUE_FLICK_VELOCITY 450f,
// DISMISS_STRIP_HEIGHT 32dp; horizontal-dominance rule 1.25x.

internal const val ShellSwipeThresholdDp = 72
internal const val ShellSwipeVelocityMinimumDp = 28
internal const val ShellSwipeVelocityThresholdPxPerMs = 1.2f
internal const val ShellQueueTravelMs = 420
internal const val ShellQueueCarryFraction = 0.3f
internal const val ShellQueueFlickVelocityPxPerSec = 450f
internal const val ShellDismissStripHeightDp = 32

internal fun shouldDispatchShellSwipe(
    horizontalDistancePx: Float,
    verticalDistancePx: Float,
    thresholdPx: Float,
    velocityPxPerMs: Float,
    velocityThresholdPxPerMs: Float,
    velocityMinimumPx: Float,
): Boolean {
    val horizontal = abs(horizontalDistancePx)
    val vertical = abs(verticalDistancePx)
    if (horizontal < vertical * 1.25f) return false
    return horizontal >= thresholdPx ||
        (horizontal >= velocityMinimumPx && abs(velocityPxPerMs) >= velocityThresholdPxPerMs)
}

internal fun shellCollapseProgress(
    queueOpen: Boolean,
    lyricsOpen: Boolean,
    queueSlide: Float,
    animatedCollapse: Float,
    queueDragging: Boolean,
): Float {
    val queueOwnsCollapse = !lyricsOpen &&
        (queueOpen || queueDragging || queueSlide > 0.001f)
    return if (queueOwnsCollapse) queueSlide.coerceIn(0f, 1f) else animatedCollapse.coerceIn(0f, 1f)
}

internal fun shouldCarryQueueOpen(slide: Float, velocityPxPerSec: Float): Boolean =
    slide >= ShellQueueCarryFraction || abs(velocityPxPerSec) >= ShellQueueFlickVelocityPxPerSec
