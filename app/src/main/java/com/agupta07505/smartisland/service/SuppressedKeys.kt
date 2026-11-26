/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import java.util.concurrent.ConcurrentHashMap

internal class SuppressedKeys(
    private val clock: () -> Long,
    private val ttlMs: Long,
    private val maxKeys: Int
) {
    private val marked = ConcurrentHashMap<String, Long>()

    val size: Int get() = marked.size

    fun mark(key: String) {
        val now = clock()
        marked[key] = now
        cleanup(now)
    }

    /** 0 when not held, expired keys are not swept first. */
    fun lastMarked(key: String): Long = marked[key] ?: 0L

    fun contains(key: String): Boolean {
        cleanup()
        return marked.containsKey(key)
    }

    fun remove(key: String) {
        marked.remove(key)
    }

    fun clear() = marked.clear()

    /** Drops keys older than the TTL, then the oldest ones above the cap. */
    fun cleanup(now: Long = clock()) {
        val cutoff = now - ttlMs
        marked.entries.forEach { entry ->
            if (entry.value < cutoff) marked.remove(entry.key, entry.value)
        }
        val overflow = marked.size - maxKeys
        if (overflow > 0) {
            marked.entries
                .sortedBy { it.value }
                .take(overflow)
                .forEach { marked.remove(it.key, it.value) }
        }
    }
}
