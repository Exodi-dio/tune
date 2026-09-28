package com.exodidio.tune.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `uses system mode for missing or unknown stored values`() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStorage("oled"))
    }

    @Test
    fun `restores each supported stored theme value`() {
        ThemeMode.entries.forEach { expected ->
            assertEquals(expected, ThemeMode.fromStorage(expected.storageValue))
        }
    }

    @Test
    fun `restores amoled theme from storage`() {
        assertEquals(ThemeMode.Amoled, ThemeMode.fromStorage("amoled"))
    }

    @Test
    fun `amoled theme round-trips through its storage value`() {
        assertEquals("amoled", ThemeMode.Amoled.storageValue)
        assertEquals(ThemeMode.Amoled, ThemeMode.fromStorage(ThemeMode.Amoled.storageValue))
    }
}
