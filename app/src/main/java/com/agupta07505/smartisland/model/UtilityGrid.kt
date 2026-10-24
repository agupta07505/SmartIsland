/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** IDs are persisted in quick-launch layouts. */
enum class UtilityType(override val id: String) : StableId {
    MobileData("mobile_data"),
    WiFi("wifi"),
    Hotspot("hotspot"),
    Flashlight("flashlight"),
    Calculator("calculator"),
    ClearCache("clear_cache"),
    ScreenRecorder("screen_recorder"),
    Screenshot("screenshot"),
    Timer("timer"),
    Stopwatch("stopwatch"),
    LockScreen("lock_screen"),
    VoiceRecorder("voice_recorder"),
    Camera("camera"),
    Bluetooth("bluetooth"),
    AirplaneMode("airplane_mode"),
    AutoRotate("auto_rotate"),
    DarkMode("dark_mode"),
    BatterySaver("battery_saver"),
    AppWidget("app_widget");

    val resizable: Boolean get() = this == AppWidget

    companion object {
        fun fromId(id: String?): UtilityType? = enumByIdOrNull(id)

        val singles: List<UtilityType> = entries.filter { it != AppWidget }
    }
}

object UtilityGrid {
    const val ROWS = 2
    const val COLUMNS = 4

    const val TABS = 2
    const val TOTAL_ROWS = ROWS * TABS
    const val NO_WIDGET = -1

    fun tabOfRow(row: Int): Int = row / ROWS

    fun localRow(row: Int): Int = row % ROWS
}

data class UtilityPlacement(
    val type: UtilityType,
    val row: Int,
    val col: Int,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val widgetId: Int = UtilityGrid.NO_WIDGET
) {
    val key: String get() = if (type == UtilityType.AppWidget) "widget:$widgetId" else type.id

    val bottom: Int get() = row + rowSpan
    val right: Int get() = col + colSpan

    val tab: Int get() = UtilityGrid.tabOfRow(row)

    val isValid: Boolean
        get() = rowSpan >= 1 && colSpan >= 1 &&
            row >= 0 && col >= 0 &&
            bottom <= UtilityGrid.TOTAL_ROWS && right <= UtilityGrid.COLUMNS &&
            UtilityGrid.tabOfRow(row) == UtilityGrid.tabOfRow(bottom - 1) &&
            (type.resizable || (rowSpan == 1 && colSpan == 1)) &&
            (type != UtilityType.AppWidget || widgetId != UtilityGrid.NO_WIDGET)

    fun overlaps(other: UtilityPlacement): Boolean =
        row < other.bottom && other.row < bottom && col < other.right && other.col < right
}

object UtilityLayout {
    private const val FIELD_SEPARATOR = ','
    private const val ITEM_SEPARATOR = ';'

    private const val VERSION_PREFIX = "v2:"

    fun encode(layout: List<UtilityPlacement>): String {
        val items = normalize(layout)
        if (items.isEmpty()) return ""
        return VERSION_PREFIX + items.joinToString(ITEM_SEPARATOR.toString()) {
            listOf(it.type.id, it.row, it.col, it.rowSpan, it.colSpan, it.widgetId)
                .joinToString(FIELD_SEPARATOR.toString())
        }
    }

    private fun UtilityPlacement.transposed(): UtilityPlacement =
        copy(row = col, col = row, rowSpan = colSpan, colSpan = rowSpan)

    fun decode(raw: String?): List<UtilityPlacement> {
        if (raw.isNullOrBlank()) return emptyList()
        val current = raw.startsWith(VERSION_PREFIX)
        val body = if (current) raw.removePrefix(VERSION_PREFIX) else raw
        val items = body.split(ITEM_SEPARATOR).mapNotNull { entry ->
            val f = entry.trim().split(FIELD_SEPARATOR)
            if (f.size < 6) return@mapNotNull null
            val type = UtilityType.fromId(f[0]) ?: return@mapNotNull null
            val nums = f.drop(1).take(5).map { it.trim().toIntOrNull() ?: return@mapNotNull null }
            UtilityPlacement(type, nums[0], nums[1], nums[2], nums[3], nums[4])
        }
        return normalize(if (current) items else items.map { it.transposed() })
    }

    fun normalize(layout: List<UtilityPlacement>): List<UtilityPlacement> {
        val kept = ArrayList<UtilityPlacement>()
        val keys = HashSet<String>()
        for (raw in layout) {
            val p = if (raw.type.resizable) raw else raw.copy(rowSpan = 1, colSpan = 1,
                widgetId = UtilityGrid.NO_WIDGET)
            if (!p.isValid) continue
            if (!keys.add(p.key)) continue
            if (kept.any { it.overlaps(p) }) { keys.remove(p.key); continue }
            kept.add(p)
        }
        return kept
    }

    fun canPlace(layout: List<UtilityPlacement>, candidate: UtilityPlacement, ignoringKey: String? = null): Boolean =
        candidate.isValid && layout.none { it.key != ignoringKey && it.overlaps(candidate) }

    fun firstFreeSpot(layout: List<UtilityPlacement>, rowSpan: Int = 1, colSpan: Int = 1): Pair<Int, Int>? {
        for (tab in 0 until UtilityGrid.TABS) {
            for (local in 0..UtilityGrid.ROWS - rowSpan) {
                val r = tab * UtilityGrid.ROWS + local
                for (c in 0..UtilityGrid.COLUMNS - colSpan) {
                    val probe = UtilityPlacement(UtilityType.AppWidget, r, c, rowSpan, colSpan, 0)
                    if (layout.none { it.overlaps(probe) }) return r to c
                }
            }
        }
        return null
    }

    fun add(layout: List<UtilityPlacement>, item: UtilityPlacement): List<UtilityPlacement> =
        if (layout.any { it.key == item.key } || !canPlace(layout, item)) layout else layout + item

    fun remove(layout: List<UtilityPlacement>, key: String): List<UtilityPlacement> = layout.filter { it.key != key }

    /** Moves the item to ([row], [col]) keeping its size, unchanged when it would not fit. */
    fun move(layout: List<UtilityPlacement>, key: String, row: Int, col: Int): List<UtilityPlacement> {
        val item = layout.firstOrNull { it.key == key } ?: return layout
        val moved = item.copy(row = row, col = col)
        return if (canPlace(layout, moved,
            ignoringKey = key)) layout.map { if (it.key == key) moved else it } else layout
    }

    fun drop(layout: List<UtilityPlacement>, key: String, row: Int, col: Int): List<UtilityPlacement> {
        val item = layout.firstOrNull { it.key == key } ?: return layout
        if (row == item.row && col == item.col) return layout
        val moved = item.copy(row = row, col = col)
        if (canPlace(layout, moved, ignoringKey = key)) return layout.map { if (it.key == key) moved else it }
        val other = layout.filter { it.key != key && it.overlaps(moved) }.singleOrNull() ?: return layout
        val dragged = item.copy(row = other.row, col = other.col)
        val traded = other.copy(row = item.row, col = item.col)
        val rest = layout.filter { it.key != key && it.key != other.key }
        val fits = dragged.isValid && traded.isValid && !dragged.overlaps(traded) &&
            rest.none { it.overlaps(dragged) || it.overlaps(traded) }
        if (!fits) return layout
        return layout.map {
            when (it.key) {
                key -> dragged
                other.key -> traded
                else -> it
            }
        }
    }

    fun snapCell(item: UtilityPlacement, x: Float, y: Float, pitchX: Float, pitchY: Float): Pair<Int, Int> {
        if (pitchX <= 0f || pitchY <= 0f) return item.row to item.col
        val col = Math.round(x / pitchX).coerceIn(0, maxOf(0, UtilityGrid.COLUMNS - item.colSpan))
        val local = Math.round(y / pitchY).coerceIn(0, maxOf(0, UtilityGrid.ROWS - item.rowSpan))
        return item.tab * UtilityGrid.ROWS + local to col
    }

    /** Resizes from the top-left corner, unchanged for a fixed-size utility or when it would not fit. */
    fun resize(layout: List<UtilityPlacement>, key: String, rowSpan: Int, colSpan: Int): List<UtilityPlacement> {
        val item = layout.firstOrNull { it.key == key } ?: return layout
        if (!item.type.resizable) return layout
        val resized = item.copy(rowSpan = rowSpan, colSpan = colSpan)
        return if (canPlace(layout, resized,
            ignoringKey = key)) layout.map { if (it.key == key) resized else it } else layout
    }

    fun usedRows(layout: List<UtilityPlacement>): Int =
        layout.filter { it.tab == 0 }.maxOfOrNull { it.bottom } ?: 0

    fun isTabFull(layout: List<UtilityPlacement>, tab: Int): Boolean {
        val base = tab * UtilityGrid.ROWS
        for (r in base until base + UtilityGrid.ROWS) for (c in 0 until UtilityGrid.COLUMNS) {
            if (itemAt(layout, r, c) == null) return false
        }
        return true
    }

    /** Tabs the page shows: the second exists only once the first is full, so an unfilled page stays single. */
    fun tabCount(layout: List<UtilityPlacement>): Int = if (isTabFull(layout, 0)) UtilityGrid.TABS else 1

    fun itemAt(layout: List<UtilityPlacement>, row: Int, col: Int): UtilityPlacement? =
        layout.firstOrNull { row >= it.row && row < it.bottom && col >= it.col && col < it.right }

    fun addable(layout: List<UtilityPlacement>): List<UtilityType> =
        UtilityType.singles.filter { type -> layout.none { it.type == type } }
}
