/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Devices on the hotspot, counted from the neighbour table of the tether interface (needs a shell). */
object HotspotClients {
    private const val NEIGHBOURS_COMMAND = "ip neigh show"
    private val LIVE_STATES = setOf("REACHABLE", "STALE", "DELAY", "PROBE")
    private val SPACES = Regex("\\s+")

    fun countFromNeighbours(output: String): Int {
        val macs = HashSet<String>()
        for (line in output.lineSequence()) {
            val parts = line.trim().split(SPACES)
            val ip = parts.firstOrNull().orEmpty()
            if (ip.contains(':') || !ip.contains('.')) continue
            val dev = parts.valueAfter("dev") ?: continue
            if (!HotspotStateMonitor.isTetherInterface(dev)) continue
            val mac = parts.valueAfter("lladdr") ?: continue
            if (parts.last() !in LIVE_STATES) continue
            macs += mac.lowercase()
        }
        return macs.size
    }

    /** Null when no shell is available */
    suspend fun read(): Int? {
        if (!ShizukuManager.isBinderAvailable() || !ShizukuManager.hasPermission()) return null
        val output = ShizukuManager.runShellForOutput(NEIGHBOURS_COMMAND).getOrNull() ?: return null
        return countFromNeighbours(output)
    }

    private fun List<String>.valueAfter(key: String): String? {
        val at = indexOf(key)
        return if (at >= 0) getOrNull(at + 1) else null
    }
}
