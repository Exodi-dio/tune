package com.exodidio.tune

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModelsStabilityTest {
    @Test
    fun aggregatesAreImmutable() {
        val immutables = listOf(
            HomeDestinationModel::class,
            LibraryTracksModel::class,
            LibraryAlbumsModel::class,
            LibraryDestinationModel::class,
            AppDestinationModels::class,
            PlaybackModel::class,
        )
        immutables.forEach { k ->
            assertTrue(
                "${k.simpleName} must carry @Immutable or @Stable",
                k.annotations.any { it is Immutable || it is Stable },
            )
        }
    }
}
