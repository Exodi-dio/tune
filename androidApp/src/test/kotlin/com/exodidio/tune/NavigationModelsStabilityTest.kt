package com.exodidio.tune

import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModelsStabilityTest {
    @Test
    fun aggregatesAreImmutable() {
        val classes = listOf(
            HomeDestinationModel::class.java,
            LibraryTracksModel::class.java,
            LibraryAlbumsModel::class.java,
            LibraryDestinationModel::class.java,
            AppDestinationModels::class.java,
            PlaybackModel::class.java,
        )
        classes.forEach { c ->
            assertTrue(
                "${c.simpleName} must carry @Immutable or @Stable",
                hasImmutableOrStableMarker(c),
            )
        }
    }

    // @Immutable/@Stable use BINARY retention: invisible to java.lang.reflect
    // but stored in the class file as RuntimeInvisibleAnnotations. Read the
    // bytes so RED fails before annotations and GREEN passes after.
    private fun hasImmutableOrStableMarker(c: Class<*>): Boolean {
        val path = c.name.replace('.', '/') + ".class"
        val stream = c.classLoader?.getResourceAsStream(path) ?: return false
        stream.use { input ->
            val bytes = input.readBytes().toString(Charsets.ISO_8859_1)
            return bytes.contains("compose/runtime/Immutable") || bytes.contains("compose/runtime/Stable")
        }
    }
}
