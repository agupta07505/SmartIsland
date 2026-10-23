/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

private fun IslandNotification.canGroupByApp(): Boolean = when (mode) {
    IslandMode.Empty,
    IslandMode.Battery,
    IslandMode.Bluetooth,
    IslandMode.Hotspot,
    IslandMode.Flashlight -> false
    else -> true
}

fun isSameAppSibling(a: IslandNotification, b: IslandNotification): Boolean =
    a.key != b.key &&
        a.canGroupByApp() && b.canGroupByApp() &&
        a.packageName == b.packageName

/**
 * Card the secondary cutout shows while expanded: the next card after [currentIndex] (wrapping) from the same app. -1
 * when the current card has no sibling.
 */
fun nextSameAppIndex(cards: List<IslandNotification>, currentIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    for (step in 1 until cards.size) {
        val index = (currentIndex + step) % cards.size
        if (isSameAppSibling(current, cards[index])) return index
    }
    return -1
}

/** Index of the card the secondary round cutout shows, or -1 when it is hidden. */
fun secondaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Int {
    if (cards.size < 2) return -1
    if (expanded) {
        val sibling = nextSameAppIndex(cards, currentIndex)
        if (sibling >= 0) return sibling
    }
    // One app, one cutout: a card from the current card's app never takes the secondary cutout on its own (expanded, a
    // sibling already returned above as the picker button).
    val current = cards.getOrNull(currentIndex)
    return cards.indexOfFirst {
        it.key != current?.key && !(current != null && isSameAppSibling(current, it)) && showsInCutout(it, cards, currentIndex)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun showsInCutout(card: IslandNotification, cards: List<IslandNotification>, currentIndex: Int): Boolean = true

/**
 * How many cutouts compete for space. One app is ONE entry; the exception is the sibling picker (expanded, with a
 * same-app sibling), which counts as an extra entry.
 */
fun cutoutCardCount(cards: List<IslandNotification>, expanded: Boolean, currentIndex: Int = 0): Int {
    val apps = cards.filter { showsInCutout(it, cards, currentIndex) }.map { appGroupKey(it) }.distinct().size
    return if (siblingPickerCutoutActive(cards, currentIndex, expanded)) apps + 1 else apps
}

/** Index of the card the tertiary round cutout shows, or -1 for none. */
fun tertiaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, secondaryIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    val taken = HashSet<String>()
    taken += appGroupKey(current)
    cards.getOrNull(secondaryIndex)?.let { taken += appGroupKey(it) }
    return cards.indexOfFirst { appGroupKey(it) !in taken && showsInCutout(it, cards, currentIndex) }
}

// App groups: switching events treats all cards of one app as ONE entry; only the sibling gestures and the sibling
// picker move between the cards inside it.

/**
 * The entry a card belongs to when switching events: one per app. Cards that cannot group (battery, Bluetooth,
 * hotspot, flashlight, empty) are an entry of their own.
 */
fun appGroupKey(card: IslandNotification): String =
    if (card.canGroupByApp()) "app:${card.packageName}" else "card:${card.key}"

/**
 * Indices of every card in [cards] from the same app as the card at [currentIndex], itself included, in list order.
 * Empty when it has no sibling.
 */
fun siblingIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val current = cards.getOrNull(currentIndex) ?: return emptyList()
    val result = cards.indices.filter { it == currentIndex || isSameAppSibling(current, cards[it]) }
    return if (result.size > 1) result else emptyList()
}

/**
 * Next (or previous) sibling of the card at [currentIndex], wrapping inside the app's own cards, or -1 when it has
 * none. For the sibling gestures, never event switching.
 */
fun nextSiblingIndex(cards: List<IslandNotification>, currentIndex: Int, forward: Boolean): Int {
    val siblings = siblingIndices(cards, currentIndex)
    if (siblings.isEmpty()) return -1
    val position = siblings.indexOf(currentIndex)
    val step = if (forward) 1 else -1
    return siblings[Math.floorMod(position + step, siblings.size)]
}

/**
 * Card to show when moving to the next (or previous) app entry after [currentIndex]: the entry's first card, so a
 * pile of siblings costs one swipe. -1 when every card belongs to one entry.
 */
fun nextAppGroupIndex(cards: List<IslandNotification>, currentIndex: Int, forward: Boolean): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    val groups = cards.map { appGroupKey(it) }.distinct()
    if (groups.size < 2) return -1
    val position = groups.indexOf(appGroupKey(current))
    val step = if (forward) 1 else -1
    val target = groups[Math.floorMod(position + step, groups.size)]
    return cards.indexOfFirst { appGroupKey(it) == target }
}

/**
 * Cards behind the expanded pager's pages: one per app group, so a swipe never walks through siblings (the picker
 * does). The current group shows the current card, the others their first.
 */
fun appGroupPageIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val firstByGroup = LinkedHashMap<String, Int>()
    cards.forEachIndexed { index, card -> firstByGroup.putIfAbsent(appGroupKey(card), index) }
    cards.getOrNull(currentIndex)?.let { firstByGroup[appGroupKey(it)] = currentIndex }
    return firstByGroup.values.toList()
}

/** Pager page (see [appGroupPageIndices]) that shows the card at [cardIndex]; 0 if none. */
fun appGroupPageOf(cards: List<IslandNotification>, cardIndex: Int): Int {
    val card = cards.getOrNull(cardIndex) ?: return 0
    val key = appGroupKey(card)
    return cards.map { appGroupKey(it) }.distinct().indexOf(key).coerceAtLeast(0)
}

/**
 * True when the expanded island's secondary cutout is the sibling picker button: expanded, and the current card has a
 * same-app sibling. The screen-share cast icon gives that cutout up while this holds.
 */
fun siblingPickerCutoutActive(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Boolean =
    expanded && nextSameAppIndex(cards, currentIndex) >= 0

/** True when a second round cutout sits beside the main one (a second card exists). Never in notch mode. */
fun hasSecondaryCutout(notificationCount: Int, notchMode: Boolean): Boolean =
    !notchMode && notificationCount >= 2
