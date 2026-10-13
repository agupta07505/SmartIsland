package com.srcpolygot.dynamicisland.domain.model

data class IslandNotification(
    val key: String,
    val packageName: String = "",
    val title: String? = null,
    val text: String? = null,
    val mode: IslandMode = IslandMode.Notification,
    val isOngoing: Boolean = false,
    val progress: Float? = null
)
