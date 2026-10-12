/*
 * Realistic Dynamic Island Fork
 * Supports up to 3 simultaneous activities like iPhone 18 Pro Max
 */

package com.srcpolygot.dynamicisland.domain.model

/**
 * Holds the current set of activities shown in the island.
 * Primary = main expanded/collapsed content
 * Secondary + Tertiary = compact companion indicators (max 3 total)
 */
data class ActiveIslandState(
    val primary: IslandNotification? = null,
    val secondary: IslandNotification? = null,
    val tertiary: IslandNotification? = null,
    val isExpanded: Boolean = false
) {
    val activeCount: Int
        get() = listOfNotNull(primary, secondary, tertiary).size

    val allActivities: List<IslandNotification>
        get() = listOfNotNull(primary, secondary, tertiary)

    fun withPrimary(notification: IslandNotification?): ActiveIslandState =
        copy(primary = notification)

    fun promoteToPrimary(key: String): ActiveIslandState {
        val target = allActivities.find { it.key == key } ?: return this
        val others = allActivities.filter { it.key != key }
        return ActiveIslandState(
            primary = target,
            secondary = others.getOrNull(0),
            tertiary = others.getOrNull(1),
            isExpanded = isExpanded
        )
    }
}
