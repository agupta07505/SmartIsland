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

fun nextSameAppIndex(cards: List<IslandNotification>, currentIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    for (step in 1 until cards.size) {
        val index = (currentIndex + step) % cards.size
        if (isSameAppSibling(current, cards[index])) return index
    }
    return -1
}

fun secondaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Int {
    if (TorchCard.open(cards.getOrNull(currentIndex)?.mode, expanded)) return TorchCard.ringingCallIndex(cards)
    if (cards.size < 2) return -1
    if (expanded) {
        val sibling = nextSameAppIndex(cards, currentIndex)
        if (sibling >= 0) return sibling
    }
    val current = cards.getOrNull(currentIndex)
    return cards.indexOfFirst {
        it.key != current?.key && !(current != null && isSameAppSibling(current, it))
    }
}

fun cutoutCardCount(cards: List<IslandNotification>, expanded: Boolean, currentIndex: Int = 0): Int {
    if (TorchCard.open(cards.getOrNull(currentIndex)?.mode, expanded)) return if (TorchCard.ringingCallIndex(cards) >= 0) 2 else 0
    val apps = cards.map { appGroupKey(it) }.distinct().size
    return if (siblingPickerCutoutActive(cards, currentIndex, expanded)) apps + 1 else apps
}

fun tertiaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, secondaryIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    val taken = HashSet<String>()
    taken += appGroupKey(current)
    cards.getOrNull(secondaryIndex)?.let { taken += appGroupKey(it) }
    return cards.indexOfFirst { appGroupKey(it) !in taken }
}

fun appGroupKey(card: IslandNotification): String =
    if (card.canGroupByApp()) "app:${card.packageName}" else "card:${card.key}"

fun siblingIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val current = cards.getOrNull(currentIndex) ?: return emptyList()
    val result = cards.indices.filter { it == currentIndex || isSameAppSibling(current, cards[it]) }
    return if (result.size > 1) result else emptyList()
}

fun nextSiblingIndex(cards: List<IslandNotification>, currentIndex: Int, forward: Boolean): Int {
    val siblings = siblingIndices(cards, currentIndex)
    if (siblings.isEmpty()) return -1
    val position = siblings.indexOf(currentIndex)
    val step = if (forward) 1 else -1
    return siblings[Math.floorMod(position + step, siblings.size)]
}

fun nextAppGroupIndex(cards: List<IslandNotification>, currentIndex: Int, forward: Boolean): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    val groups = cards.map { appGroupKey(it) }.distinct()
    if (groups.size < 2) return -1
    val position = groups.indexOf(appGroupKey(current))
    val step = if (forward) 1 else -1
    val target = groups[Math.floorMod(position + step, groups.size)]
    return cards.indexOfFirst { appGroupKey(it) == target }
}

fun appGroupPageIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val firstByGroup = LinkedHashMap<String, Int>()
    cards.forEachIndexed { index, card -> firstByGroup.putIfAbsent(appGroupKey(card), index) }
    cards.getOrNull(currentIndex)?.let { firstByGroup[appGroupKey(it)] = currentIndex }
    return firstByGroup.values.toList()
}

fun appGroupPageOf(cards: List<IslandNotification>, cardIndex: Int): Int {
    val card = cards.getOrNull(cardIndex) ?: return 0
    val key = appGroupKey(card)
    return cards.map { appGroupKey(it) }.distinct().indexOf(key).coerceAtLeast(0)
}

fun siblingPickerCutoutActive(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Boolean =
    expanded && nextSameAppIndex(cards, currentIndex) >= 0

fun hasSecondaryCutout(notificationCount: Int): Boolean = notificationCount >= 2
