package com.exodidio.tune

import com.exodidio.tune.sync.LibraryGenre
import com.exodidio.tune.ui.screens.GenreSortOption
import com.exodidio.tune.ui.screens.SortOrder
import com.exodidio.tune.ui.screens.sortGenres
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryGenresViewModelTest {
    private val genres = listOf(
        LibraryGenre(id = "1", name = "Rock"),
        LibraryGenre(id = "2", name = "Ambient"),
        LibraryGenre(id = "3", name = "Classical"),
    )

    @Test
    fun sortsByNameInBothDirections() {
        assertEquals(
            listOf("Ambient", "Classical", "Rock"),
            sortGenres(genres, GenreSortOption.Name, SortOrder.Ascending).map { it.name },
        )
        assertEquals(
            listOf("Rock", "Classical", "Ambient"),
            sortGenres(genres, GenreSortOption.Name, SortOrder.Descending).map { it.name },
        )
    }

    @Test
    fun sortsByDateAddedWithNameTieBreaker() {
        assertEquals(
            listOf("Classical", "Rock", "Ambient"),
            sortGenres(genres, GenreSortOption.DateAdded, SortOrder.Ascending).map { it.name },
        )
    }

    @Test
    fun usesNormalizationDerivedSortNameInsteadOfDisplayName() {
        val genres = listOf(
            LibraryGenre(id = "1", name = "Zulu"),
            LibraryGenre(id = "2", name = "Alpha"),
        )

        assertEquals(listOf("Zulu", "Alpha"), sortGenres(genres, GenreSortOption.Name, SortOrder.Ascending).map { it.name })
    }
}
