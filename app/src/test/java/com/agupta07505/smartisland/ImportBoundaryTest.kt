/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Layers point 1 way: `model` is plain data and rules, `util` is helpers for services and screens, `data` is
 * storage. A lower layer importing a higher one fails here, with file and line.
 *
 * Reads import lines only, a class name in a string (see `AppComponents`) is not an import, that is how a lower
 * layer names a component without depending on it
 */
class ImportBoundaryTest {
    private val appPackage = "com.agupta07505.smartisland"

    private val entryPoints = setOf("MainActivity", "SmartIslandApp")

    @Test
    fun layerImportBoundaries() {
        assertNoForbiddenImports("model", setOf("data", "util", "service", "ui", "receiver", "di") + entryPoints)
        assertNoForbiddenImports("util", setOf("service", "ui", "receiver") + entryPoints)
        assertNoForbiddenImports("data", setOf("di", "service", "ui", "receiver") + entryPoints)
    }

    @Test
    fun sourceLayerCoverage() {
        for (layer in listOf("model", "util", "data")) {
            assertTrue("no source files found for layer $layer", layerFiles(layer).isNotEmpty())
        }
    }

    @Test
    fun detectorFlagsRealImportsOnly() {
        val source = """
            package $appPackage.model

            import java.util.Locale
            import $appPackage.R
            import $appPackage.model.IslandMode
            import $appPackage.data.SmartIslandSettings
            import $appPackage.util.Log as AppLog
            import $appPackage.service.*
              import $appPackage.MainActivity
            // import $appPackage.ui.Something
            val name = "$appPackage.receiver.TimerActionReceiver"
        """.trimIndent()

        val hits = forbiddenImports(source, setOf("data", "util", "service", "ui", "receiver", "MainActivity"))

        assertEquals(
            listOf(
                "line 6: import $appPackage.data.SmartIslandSettings",
                "line 7: import $appPackage.util.Log as AppLog",
                "line 8: import $appPackage.service.*",
                "line 9: import $appPackage.MainActivity"
            ),
            hits
        )
    }

    private fun assertNoForbiddenImports(layer: String, forbidden: Set<String>) {
        val violations = layerFiles(layer).flatMap { file ->
            forbiddenImports(file.readText(), forbidden).map { "${file.name} ${it}" }
        }
        assertTrue(
            "$layer must not import ${forbidden.sorted()}, found:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun forbiddenImports(source: String, forbidden: Set<String>): List<String> =
        source.lines().mapIndexedNotNull { index, line ->
            val target = IMPORT.find(line)?.groupValues?.get(1)
            if (target != null && target in forbidden) "line ${index + 1}: ${line.trim()}" else null
        }

    private fun layerFiles(layer: String): List<File> {
        val packagePath = appPackage.replace('.', '/')
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(System.getProperty("user.dir"), "$it/$packagePath") }
            .firstOrNull { it.isDirectory }
            ?: throw AssertionError("source root not found from ${System.getProperty("user.dir")}")
        return File(root, layer).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private companion object {
        /** The first name after the app package: `data` in `<app>.data.X`, `MainActivity` in `<app>.MainActivity` */
        val IMPORT = Regex("""^\s*import\s+com\.agupta07505\.smartisland\.([A-Za-z_][A-Za-z0-9_]*)""")
    }
}
