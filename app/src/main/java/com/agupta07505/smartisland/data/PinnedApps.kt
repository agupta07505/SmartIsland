/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

/**
 * The pinned quick-launch apps: an ordered list of ENTRIES, each one slot of the island grid (at most [MAX]), either a
 * package name or a folder of two to [MAX_FOLDER] apps.
 *
 * A folder is one string in the same list, `folder:pkgA,pkgB,pkgC`, so storage, backup and ordering stay a plain list of
 * strings. An app is never pinned twice, and a folder never holds fewer than two apps (it dissolves into the remaining
 * app). Everything that touches the list goes through here so these rules live in one place.
 */
object PinnedApps {
    /** Slots in the grid. A folder takes one slot, whatever it holds. */
    const val MAX = 8
    const val MAX_FOLDER = 4
    private const val MAX_PACKAGE_NAME_LENGTH = 255
    private const val FOLDER_PREFIX = "folder:"
    private const val MEMBER_SEPARATOR = ','

    private const val SEPARATOR = '\n'

    fun isFolder(entry: String): Boolean = entry.startsWith(FOLDER_PREFIX)

    fun members(entry: String): List<String> =
        if (isFolder(entry)) {
            entry.removePrefix(FOLDER_PREFIX).split(MEMBER_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(entry)
        }

    fun flatPackages(entries: List<String>): List<String> = entries.flatMap(::members)

    private fun entryFor(members: List<String>): String? = when (members.size) {
        0 -> null
        1 -> members[0]
        else -> FOLDER_PREFIX + members.joinToString(MEMBER_SEPARATOR.toString())
    }

    /**
     * Cleans a list of entries: drops blank, over-long and repeated packages (the first one wins, across folders
     * too), trims folders to [MAX_FOLDER] apps, dissolves a folder left with one app, and caps the list at [MAX]
     * entries.
     */
    fun normalize(entries: Iterable<String>): List<String> {
        val seen = HashSet<String>()
        val result = ArrayList<String>()
        for (raw in entries) {
            if (result.size >= MAX) break
            val apps = members(raw.trim())
                .filter { it.isNotEmpty() && it.length <= MAX_PACKAGE_NAME_LENGTH && it.none(Char::isWhitespace) && it !in seen }
                .distinct()
                .let { if (isFolder(raw.trim())) it.take(MAX_FOLDER) else it.take(1) }
            val entry = entryFor(apps) ?: continue
            seen.addAll(apps)
            result.add(entry)
        }
        return result
    }

    fun encode(entries: List<String>): String = normalize(entries).joinToString(SEPARATOR.toString())

    fun decode(raw: String?): List<String> =
        if (raw.isNullOrEmpty()) emptyList() else normalize(raw.split(SEPARATOR))

    /** Reads the saved order. */
    fun restore(ordered: String?, legacySet: Set<String>?): List<String> =
        if (ordered != null) decode(ordered) else normalize(legacySet.orEmpty())

    fun move(entries: List<String>, from: Int, to: Int): List<String> {
        if (from !in entries.indices || to !in entries.indices || from == to) return entries
        return entries.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun swap(entries: List<String>, entry: String, target: String): List<String> {
        val a = entries.indexOf(entry)
        val b = entries.indexOf(target)
        if (a < 0 || b < 0 || a == b) return entries
        return entries.toMutableList().apply {
            this[a] = target
            this[b] = entry
        }
    }

    fun moveOnto(entries: List<String>, entry: String, target: String): List<String> =
        move(entries, entries.indexOf(entry), entries.indexOf(target))

    fun moveInFolder(entries: List<String>, folder: String, from: Int, to: Int): List<String> {
        val at = entries.indexOf(folder)
        if (at < 0 || !isFolder(folder)) return entries
        val apps = members(folder)
        if (from !in apps.indices || to !in apps.indices || from == to) return entries
        val reordered = apps.toMutableList().apply { add(to, removeAt(from)) }
        return entries.toMutableList().apply { set(at, entryFor(reordered) ?: folder) }
    }

    fun pin(entries: List<String>, packageName: String): List<String> =
        if (packageName in flatPackages(entries) || entries.size >= MAX) entries else normalize(entries + packageName)

    /** Unpins an app wherever it is; a folder left with one app becomes that app. */
    fun unpin(entries: List<String>, packageName: String): List<String> =
        entries.mapNotNull { entry ->
            if (packageName in members(entry)) entryFor(members(entry) - packageName) else entry
        }

    fun canJoin(entries: List<String>, entry: String, target: String): Boolean =
        entry != target && !isFolder(entry) && entry in entries && target in entries &&
            (!isFolder(target) || members(target).size < MAX_FOLDER)

    /**
     * Drops the loose app [entry] onto [target]: onto another app they become a folder in the target's place, onto a
     * folder with room the app joins it.
     */
    fun joinFolder(entries: List<String>, entry: String, target: String): List<String> {
        if (!canJoin(entries, entry, target)) return entries
        val folder = entryFor(members(target) + entry) ?: return entries
        return entries.filter { it != entry }.map { if (it == target) folder else it }
    }

    /**
     * Whether dragging an app out of a folder cannot keep it pinned: the app needs a slot of its own, and when all
     * [MAX] slots are taken there is none.
     */
    fun dragOutUnpins(entries: List<String>): Boolean = entries.size >= MAX

    /**
     * Drags [packageName] out of [folder]. It gets a slot of its own right after the folder, or, when no slot is
     * free, is unpinned. The folder keeps the others, or becomes the single app that is left.
     */
    fun dragOut(entries: List<String>, folder: String, packageName: String): List<String> {
        val at = entries.indexOf(folder)
        if (at < 0 || !isFolder(folder) || packageName !in members(folder)) return entries
        val rest = entryFor(members(folder) - packageName)
        val keep = !dragOutUnpins(entries)
        return buildList {
            addAll(entries.subList(0, at))
            rest?.let { add(it) }
            if (keep) add(packageName)
            addAll(entries.subList(at + 1, entries.size))
        }
    }
}
