package com.srcpolygot.dynamicisland.domain.model

/**
 * Holds up to 3 simultaneous activities – matches iPhone 18 Pro Max Dynamic Island.
 */
data class ActiveIslandState(
    val primary: IslandNotification? = null,
    val secondary: IslandNotification? = null,
    val tertiary: IslandNotification? = null,
    val isExpanded: Boolean = false
) {
    val hasActivity: Boolean
        get() = primary != null

    val activityCount: Int
        get() = listOfNotNull(primary, secondary, tertiary).size
}
