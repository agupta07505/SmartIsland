/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwapPickerPolicy
import com.agupta07505.smartisland.model.siblingIndices
import com.agupta07505.smartisland.util.SwapTrace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * The sibling picker of the expanded card: whether it is open, the request that waits for a running swap to settle,
 * and the rules that close it. The view model owns the selection, so picking a card goes through [selectByUser]
 */
internal class IslandSiblingPicker(
    private val expanded: StateFlow<Boolean>,
    private val isInputActive: StateFlow<Boolean>,
    private val visibleNotifications: StateFlow<List<IslandNotification>>,
    private val selectedIndex: StateFlow<Int>,
    private val swapOwnsSelection: StateFlow<Boolean>,
    private val selectByUser: (Int) -> Unit
) {
    val open = MutableStateFlow(false)

    /** The picker was asked for while a swap ran, it opens when the swap has settled. */
    private var openPending = false

    /** The island closed: the picker and a request waiting for a swap go with it. */
    fun reset() {
        open.value = false
        openPending = false
    }

    fun openPicker(): Boolean {
        if (!expanded.value || isInputActive.value) return false
        if (siblingIndices(visibleNotifications.value, selectedIndex.value).isEmpty()) return false
        if (swapOwnsSelection.value) {
            openPending = true
            return true
        }
        open.value = true
        return true
    }

    fun closePicker() {
        if (!open.value || swapOwnsSelection.value) return
        open.value = false
    }

    fun selectSibling(key: String): Boolean {
        if (swapOwnsSelection.value) return false
        val index = visibleNotifications.value.indexOfFirst { it.key == key }
        if (index < 0) return false
        selectByUser(index)
        open.value = false
        return true
    }

    /** Opens the picker after a swap and closes it when its card is gone; runs until the scope is cancelled. */
    suspend fun collect() {
        // The picker belongs to 1 card's app, a swap changing the card on its way does not end that
        var pickerCardKey: String? = null
        var wasSwapping = false
        combine(open, visibleNotifications, selectedIndex, swapOwnsSelection) { open, list, index, swapping ->
            Pair(Triple(open, list, index), swapping)
        }.collect { (state, swapping) ->
            val (open, list, index) = state
            val settledNow = wasSwapping && !swapping
            wasSwapping = swapping
            val key = list.getOrNull(index)?.key
            val siblings = siblingIndices(list, index).isNotEmpty()
            val opensNow = SwapPickerPolicy.opensAfterSwap(openPending, settledNow, expanded.value, siblings)
            if (settledNow) openPending = false
            if (opensNow && !open) {
                this.open.value = true
                pickerCardKey = key
                return@collect
            }
            val result = SwapPickerPolicy.resolve(open, pickerCardKey, key, siblings, swapping, settledNow)
            pickerCardKey = result.anchorKey
            if (open && !result.open) {
                SwapTrace.log("picker-close") { "key=$key anchor=${result.anchorKey} siblings=$siblings index=$index" }
                this.open.value = false
            }
        }
    }
}
