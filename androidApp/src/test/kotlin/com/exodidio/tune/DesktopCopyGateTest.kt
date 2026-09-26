package com.exodidio.tune

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopCopyGateTest {
    private val localeDirs = listOf(
        "values", "values-en", "values-de", "values-es", "values-fr", "values-it",
        "values-pt", "values-ru", "values-ja", "values-ko", "values-th", "values-vi", "values-zh",
    )
    private val rewrittenKeys = listOf(
        "playlists_empty_description",
        "playlist_sync_failed",
        "library_empty_title",
        "library_empty_description",
    )
    private val banned = listOf(
        "desktop", "escritorio", "ordinateur", "bureau", "computador", "computer",
        "компьютер", "デスクトップ", "데스크톱", "เดสก์ท็อป", "máy tính", "电脑",
    )

    @Test
    fun rewrittenEmptyStateCopyNeverReferencesADesktop() {
        val resDir = File(System.getProperty("user.dir"), "src/main/res")
        assertTrue("res dir missing: ${resDir.absolutePath}", resDir.isDirectory)
        val violations = mutableListOf<String>()
        val factory = DocumentBuilderFactory.newInstance()
        for (locale in localeDirs) {
            val file = File(resDir, "$locale/strings.xml")
            assertTrue("missing $locale/strings.xml", file.isFile)
            val doc = factory.newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            val values = mutableMapOf<String, String>()
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name").nodeValue
                if (name in rewrittenKeys) {
                    values[name] = node.textContent
                }
            }
            for (key in rewrittenKeys) {
                val value = values[key]
                if (value == null) {
                    violations += "$locale:$key MISSING"
                    continue
                }
                val lowered = value.lowercase(Locale.ROOT)
                val hit = banned.firstOrNull { lowered.contains(it.lowercase(Locale.ROOT)) }
                if (hit != null) {
                    violations += "$locale:$key contains \"$hit\": $value"
                }
            }
        }
        assertTrue(
            "Desktop references in empty-state copy:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
