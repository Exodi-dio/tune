package com.exodidio.tune

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModelsStabilityTest {
    @Test
    fun aggregatesAreImmutable() {
        val immutables = listOf(
            HomeDestinationModel::class.java,
            LibraryTracksModel::class.java,
            LibraryAlbumsModel::class.java,
            LibraryDestinationModel::class.java,
            AppDestinationModels::class.java,
            PlaybackModel::class.java,
        )
        immutables.forEach { c ->
            assertTrue(
                "${c.simpleName} must carry @Immutable or @Stable",
                c.isAnnotationPresent(Immutable::class.java) || c.isAnnotationPresent(Stable::class.java),
            )
        }
    }
}
