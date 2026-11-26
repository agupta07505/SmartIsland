/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.ui.IslandViewModel

/**
 * Shared slot for the [IslandViewModel] the service builds in onCreate, helpers and system callbacks can get here
 * before that or after it failed. Replaces the viewModelProvider / isViewModelReady lambda pair they were wired with
 */
internal class OverlayViewModelHolder {
    @Volatile
    private var model: IslandViewModel? = null

    val ready: Boolean get() = model != null

    fun set(viewModel: IslandViewModel) {
        model = viewModel
    }

    fun get(): IslandViewModel = checkNotNull(model) { "IslandViewModel is not created yet" }
}
