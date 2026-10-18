/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

internal class OverlayViewTreeOwners : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var restored = false

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry

    fun resume() {
        if (!restored) {
            savedStateController.performRestore(null)
            restored = true
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /**
     * Screen off: drop to CREATED, not just STARTED. Compose's window recomposer pauses its frame clock on
     * ON_STOP, which is what actually halts `infiniteRepeatable` animations; STARTED would keep them ticking.
     * [resume] passes ON_START again and the clock restarts.
     */
    fun pause() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
