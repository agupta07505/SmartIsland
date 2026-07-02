package com.agupta07505.smartisland.data

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f
) {
    companion object {
        val Default = SmartIslandSettings()
    }
}
