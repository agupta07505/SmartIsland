/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShortcutActionTest {
    @Test
    fun menuListsAllThreeActionsInOrderWhenFloatingIsOn() {
        val expected = listOf(AppShortcutAction.AppInfo, AppShortcutAction.FloatingWindow, AppShortcutAction.SplitScreen)
        assertEquals(expected, appShortcutMenu(FloatingWindowRoute.System))
        assertEquals(expected, appShortcutMenu(FloatingWindowRoute.Lmo))
    }

    @Test
    fun menuHidesFloatingWindowWhenItIsOff() {
        assertEquals(
            listOf(AppShortcutAction.AppInfo, AppShortcutAction.SplitScreen),
            appShortcutMenu(FloatingWindowRoute.Off)
        )
    }

    @Test
    fun anAppInTheForegroundIsPairedWithTheChosenApp() {
        assertEquals(
            SplitPlan.PairWithForeground,
            splitScreenPlan("com.chat", setOf("com.launcher"), "com.maps")
        )
    }

    @Test
    fun fromTheHomeScreenAndroidsOwnSelectionTakesOver() {
        assertEquals(
            SplitPlan.LaunchThenSystemPicker,
            splitScreenPlan("com.launcher", setOf("com.launcher", "com.other.home"), "com.maps")
        )
    }

    @Test
    fun noKnownForegroundFallsBackToTheSystemSelection() {
        assertEquals(SplitPlan.LaunchThenSystemPicker, splitScreenPlan(null, emptySet(), "com.maps"))
        assertEquals(SplitPlan.LaunchThenSystemPicker, splitScreenPlan("", emptySet(), "com.maps"))
    }

    @Test
    fun theChosenAppAlreadyOpenCannotBeSplitWithItself() {
        assertEquals(
            SplitPlan.LaunchThenSystemPicker,
            splitScreenPlan("com.maps", setOf("com.launcher"), "com.maps")
        )
    }
}
