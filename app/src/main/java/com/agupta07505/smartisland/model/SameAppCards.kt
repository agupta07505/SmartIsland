/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Cards from the same app, used by the secondary round cutout while the island is expanded.
 *
 * Cards the island or the system makes up itself (battery, Bluetooth, hotspot, flashlight) share
 * a package such as com.android.systemui without belonging to one app, so they never group.
 */
private fun IslandNotification.canGroupByApp(): Boolean = when (mode) {
    IslandMode.Empty,
    IslandMode.Battery,
    IslandMode.Bluetooth,
    IslandMode.Hotspot,
    IslandMode.Flashlight -> false
    else -> true
}

/** True when [a] and [b] are different cards from the same app. */
fun isSameAppSibling(a: IslandNotification, b: IslandNotification): Boolean =
    a.key != b.key &&
        a.canGroupByApp() && b.canGroupByApp() &&
        a.packageName == b.packageName

/**
 * Index of the card the secondary round cutout shows while the island is expanded: the next card
 * after [currentIndex] (wrapping around) that comes from the same app as the current one.
 * Tapping the cutout selects it, and the cutout then points at the following one. -1 when the
 * current card has no same-app sibling.
 */
fun nextSameAppIndex(cards: List<IslandNotification>, currentIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    for (step in 1 until cards.size) {
        val index = (currentIndex + step) % cards.size
        if (isSameAppSibling(current, cards[index])) return index
    }
    return -1
}

/**
 * Index of the card the secondary round cutout shows, or -1 when it is hidden.
 *
 * Collapsed island: the first card from a DIFFERENT app than the current one (siblings of the current
 * app never take a second cutout; it merges away after
 * [com.agupta07505.smartisland.util.IslandTimings.SECONDARY_IDLE_MS]).
 * Expanded island: a card from the same app as the current one when there is one (the cutout is
 * then the sibling picker button). With no same-app sibling it falls back to the first other card,
 * so the collapsed pill / secondary cutout stay visible beside the open card instead of vanishing.
 */
fun secondaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Int {
    if (cards.size < 2) return -1
    if (expanded) {
        val sibling = nextSameAppIndex(cards, currentIndex)
        if (sibling >= 0) return sibling
    }
    // One app, one cutout: a card from the same app as the current one never takes the secondary
    // cutout on its own. (Expanded, a sibling already returned above as the picker button.)
    val current = cards.getOrNull(currentIndex)
    return cards.indexOfFirst {
        it.key != current?.key && !(current != null && isSameAppSibling(current, it)) && showsInCutout(it, cards, currentIndex)
    }
}

/**
 * A battery event takes a secondary or tertiary cutout like any other card, with or without a live event on the island
 * (see [IslandPriority]), so it also shows beside a notification when nothing is live.
 */
@Suppress("UNUSED_PARAMETER")
private fun showsInCutout(card: IslandNotification, cards: List<IslandNotification>, currentIndex: Int): Boolean = true

/**
 * How many cutouts compete for the island's space. One app is ONE entry, collapsed or expanded
 * (an app never takes two cutouts). The one exception is the sibling picker: expanded, when the
 * card at [currentIndex] has a same-app sibling, the secondary cutout is the picker button and
 * counts as an extra entry on top of the app's own expanded card. Feeds [hasSecondaryCutout] and
 * the layout.
 */
fun cutoutCardCount(cards: List<IslandNotification>, expanded: Boolean, currentIndex: Int = 0): Int {
    val apps = cards.filter { showsInCutout(it, cards, currentIndex) }.map { appGroupKey(it) }.distinct().size
    return if (siblingPickerCutoutActive(cards, currentIndex, expanded)) apps + 1 else apps
}

/**
 * Index of the card the tertiary round cutout shows, or -1 for none. One app, one cutout: the
 * card must come from an app that is neither the current card's (the open card or the collapsed
 * pill) nor the secondary cutout's. The sibling picker button belongs to the current app, so it
 * is covered by the first rule.
 */
fun tertiaryCardIndex(cards: List<IslandNotification>, currentIndex: Int, secondaryIndex: Int): Int {
    val current = cards.getOrNull(currentIndex) ?: return -1
    val taken = HashSet<String>()
    taken += appGroupKey(current)
    cards.getOrNull(secondaryIndex)?.let { taken += appGroupKey(it) }
    return cards.indexOfFirst { appGroupKey(it) !in taken && showsInCutout(it, cards, currentIndex) }
}

// ---------------------------------------------------------------------------------------------
// App groups (sibling track S1). Switching between events treats all cards of one app as ONE
// entry; only the sibling gestures and the sibling picker move between the cards inside it.
// ---------------------------------------------------------------------------------------------

/**
 * The entry a card belongs to when switching events: one entry per app. Cards that cannot group
 * (battery, Bluetooth, hotspot, flashlight, empty) are an entry of their own.
 */
fun appGroupKey(card: IslandNotification): String =
    if (card.canGroupByApp()) "app:${card.packageName}" else "card:${card.key}"

/**
 * Indices of every card in [cards] that belongs to the same app as the card at [currentIndex],
 * the current one included, in list order. Empty when the current card has no sibling.
 */
fun siblingIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val current = cards.getOrNull(currentIndex) ?: return emptyList()
    val result = cards.indices.filter { it == currentIndex || isSameAppSibling(current, cards[it]) }
    return if (result.size > 1) result else emptyList()
}

/**
 * Next (or previous) sibling of the card at [currentIndex], wrapping around inside the app's own
 * cards, or -1 when it has no sibling. Used by the sibling gestures, never by event switching.
 */
fun nextSiblingIndex(cards: List<IslandNotification>, currentIndex: Int, forward: Boolean): Int {
    val siblings = siblingIndices(cards, currentIndex)
    if (siblings.isEmpty()) return -1
    val position = siblings.indexOf(currentIndex)
    val step = if (forward) 1 else -1
    return siblings[Math.floorMod(position + step, siblings.size)]
}

/**
 * Index of the card to show when moving to the next (or previous) app entry after the card at
 * [currentIndex]. Entries are the apps in order of their first card in [cards]; a group's
 * cards need not be next to each other in the list. The target is the first card of that
 * entry, so a pile of siblings costs one swipe. -1 when every card belongs to the same entry.
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
 * Cards behind the pages of the expanded island's pager: one page per app group, so a left /
 * right swipe never walks through an app's siblings (the sibling picker does that). The page of
 * the group that holds [currentIndex] shows the current card; every other group shows its first
 * card. Pages follow the order of each group's first card in [cards].
 */
fun appGroupPageIndices(cards: List<IslandNotification>, currentIndex: Int): List<Int> {
    val firstByGroup = LinkedHashMap<String, Int>()
    cards.forEachIndexed { index, card -> firstByGroup.putIfAbsent(appGroupKey(card), index) }
    cards.getOrNull(currentIndex)?.let { firstByGroup[appGroupKey(it)] = currentIndex }
    return firstByGroup.values.toList()
}

/** Page of the pager (see [appGroupPageIndices]) that shows the card at [cardIndex]; 0 if none. */
fun appGroupPageOf(cards: List<IslandNotification>, cardIndex: Int): Int {
    val card = cards.getOrNull(cardIndex) ?: return 0
    val key = appGroupKey(card)
    return cards.map { appGroupKey(it) }.distinct().indexOf(key).coerceAtLeast(0)
}

/**
 * True when the expanded island's secondary round cutout is the sibling picker button: the
 * island is expanded and the current card has a same-app sibling. The screen-share cast icon
 * gives that cutout up while this holds.
 */
fun siblingPickerCutoutActive(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): Boolean =
    expanded && nextSameAppIndex(cards, currentIndex) >= 0
