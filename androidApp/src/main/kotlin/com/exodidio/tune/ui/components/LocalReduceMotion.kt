package com.exodidio.tune.ui.components

import androidx.compose.runtime.compositionLocalOf

// Surfaced from App.kt as uiState.reduceTransparency || ActivityManager.isLowRamDevice().
// Marquee, playing indicator, and lyrics shimmer read this to emit static fallbacks.
val LocalReduceMotion = compositionLocalOf { false }
