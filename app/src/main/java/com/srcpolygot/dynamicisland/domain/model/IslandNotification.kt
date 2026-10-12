/*
 * Realistic Dynamic Island Fork
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.domain.model

data class IslandNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val mode: IslandMode = IslandMode.Notification,
    val appIcon: Any? = null,          // Bitmap or Icon
    val actions: List<String> = emptyList(),
    val isOngoing: Boolean = false,
    val progress: Float? = null,
    val mediaSessionToken: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)
