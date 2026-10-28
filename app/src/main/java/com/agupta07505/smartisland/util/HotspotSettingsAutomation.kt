/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Reads the system's hotspot pages while they are on screen (the connected-device line) and flips the hotspot switch
 * when asked. For phones that post no hotspot notification and have no shell
 */
object HotspotSettingsAutomation {
    data class Item(val text: String, val isSwitch: Boolean, val checked: Boolean, val enabled: Boolean)

    private const val TURN_OFF_WINDOW_MS = 6_000L
    private const val SCAN_GAP_MS = 400L
    private const val CLOSE_DELAY_MS = 450L
    private const val MAX_NODES = 400
    private val COUNT = Regex("""^\s*(\d+)\s+(?:connected\s+)?(?:device|client)s?(?:\s+connected)?\s*$""", RegexOption.IGNORE_CASE)
    private val NONE = Regex("""^\s*(?:no|zero)\s+(?:connected\s+)?(?:device|client)s?(?:\s+connected)?\s*$""", RegexOption.IGNORE_CASE)

    @Volatile var hotspotOn = false
    @Volatile var onCount: ((Int) -> Unit)? = null
    @Volatile var onToggled: (() -> Unit)? = null
    @Volatile private var turnOffUntilMs = 0L
    private var lastScanMs = 0L
    private val main = Handler(Looper.getMainLooper())

    fun requestTurnOff(nowMs: Long) {
        turnOffUntilMs = nowMs + TURN_OFF_WINDOW_MS
    }

    internal fun countOf(texts: List<String>): Int? {
        for (text in texts) {
            COUNT.matchEntire(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
            if (NONE.matches(text)) return 0
        }
        return null
    }

    /** The first switch that is on and can be flipped, in reading order. */
    internal fun firstOnSwitch(items: List<Item>): Int? =
        items.indexOfFirst { it.isSwitch && it.checked && it.enabled }.takeIf { it >= 0 }

    fun onWindow(root: AccessibilityNodeInfo?, nowMs: Long) {
        val turningOff = nowMs < turnOffUntilMs
        if (root == null || (!hotspotOn && !turningOff)) return
        if (root.packageName?.toString()?.contains("settings", ignoreCase = true) != true) return
        if (nowMs - lastScanMs < SCAN_GAP_MS) return
        lastScanMs = nowMs

        val nodes = ArrayList<Pair<AccessibilityNodeInfo, Item>>()
        walk(root, nodes)
        countOf(nodes.map { it.second.text })?.let { onCount?.invoke(it) }
        if (!turningOff) return

        val at = firstOnSwitch(nodes.map { it.second }) ?: return
        if (!click(nodes[at].first)) return
        turnOffUntilMs = 0L
        main.postDelayed({ UtilityBridge.globalAction?.invoke(AccessibilityService.GLOBAL_ACTION_BACK) }, CLOSE_DELAY_MS)
        onToggled?.invoke()
    }

    private fun walk(node: AccessibilityNodeInfo, out: MutableList<Pair<AccessibilityNodeInfo, Item>>) {
        if (out.size >= MAX_NODES) return
        val name = node.className?.toString().orEmpty()
        val flippable = node.isCheckable && !name.contains("CheckBox") && !name.contains("Radio")
        out += node to Item(node.text?.toString().orEmpty(), flippable, node.isChecked, node.isEnabled)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walk(child, out)
        }
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val parent = node.parent ?: return false
        return parent.isCheckable && parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
