/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.HOLD_GESTURE_THRESHOLD_MS
import com.agupta07505.smartisland.ui.CardActions
import com.agupta07505.smartisland.ui.ExpandActions
import com.agupta07505.smartisland.ui.IdleActions
import com.agupta07505.smartisland.ui.IslandActionRunner
import com.agupta07505.smartisland.ui.IslandActions
import com.agupta07505.smartisland.ui.IslandInteractionState
import com.agupta07505.smartisland.ui.IslandViewState
import com.agupta07505.smartisland.ui.IslandWiring
import com.agupta07505.smartisland.ui.PulseActions
import com.agupta07505.smartisland.ui.SiblingActions
import com.agupta07505.smartisland.ui.SystemActions
import com.agupta07505.smartisland.ui.expanded.MediaExpansion
import com.agupta07505.smartisland.ui.islandMainGestures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real `islandMainGestures` modifier on a collapsed pill, driven by injected touches.
 *
 * What is observed: the callbacks the gesture reaches, the number of vibrator look-ups
 * (`IslandHaptics.play` makes exactly one per buzz, so a count above 1 is a duplicate buzz) and the
 * pill's own animation state at rest. Time is the Compose test clock, so 300 ms is exact.
 */
@RunWith(AndroidJUnit4::class)
class MainPillHoldGestureTest {
    @get:Rule
    val compose = createComposeRule()

    private class Counts {
        var toggle = 0
        var togglePillHidden = 0
        var openApps = 0
        var repulse = 0
        var dragExpand = 0
        var other = 0
    }

    private class VibratorLookups(base: Context) : ContextWrapper(base) {
        var count = 0
        override fun getSystemService(name: String): Any? {
            if (name == Context.VIBRATOR_MANAGER_SERVICE || name == Context.VIBRATOR_SERVICE) count++
            return super.getSystemService(name)
        }
    }

    private class Rig(
        val settings: SmartIslandSettings,
        val withCard: Boolean,
        val dragExpandAccepts: Boolean,
        val glyphHolds: Boolean,
        val landscape: Boolean = false
    ) {
        val counts = Counts()
        val ui = IslandInteractionState()
        var vibrator: VibratorLookups? = null
    }

    private lateinit var rig: Rig

    private val hold = HOLD_GESTURE_THRESHOLD_MS
    private val toggleHold = SmartIslandSettings.Default.copy(enableAppShortcuts = false, longPressAction = "TogglePill")
    private val appsHold = SmartIslandSettings.Default.copy(enableAppShortcuts = true)

    @Before
    fun clockByHand() {
        compose.mainClock.autoAdvance = false
    }

    private fun wiringFor(rig: Rig): IslandWiring {
        val c = rig.counts
        val card = IslandNotification(
            key = "k", packageName = "x", appName = "x", title = "t", text = "t",
            timeMillis = 0, mode = IslandMode.Battery
        )
        val actions = IslandActions(
            expand = ExpandActions(
                toggle = { c.toggle++ },
                setExpanded = { c.other++ },
                dragExpand = { c.dragExpand++; rig.dragExpandAccepts },
                reportSurfaceBounds = {}
            ),
            card = CardActions(
                selectPage = { c.other++ },
                switchEvent = { c.other++ },
                switchNotification = { c.other++; false },
                deeper = { false },
                back = { false },
                open = { c.other++ },
                dismiss = { c.other++ },
                dismissAll = { c.other++ }
            ),
            sibling = SiblingActions(
                switchSibling = { c.other++; false },
                openPicker = { c.other++; false },
                closePicker = { c.other++ }
            ),
            idle = IdleActions(
                revealCards = { c.other++ },
                dotTap = { c.other++ },
                togglePillHidden = { c.togglePillHidden++ }
            ),
            pulse = PulseActions(repulseCurrent = { c.repulse++ }),
            system = SystemActions(
                openAppsLauncher = { c.openApps++ },
                openFloatingWindow = { c.other++ },
                openNotificationShade = { c.other++ }
            )
        )
        val view = IslandViewState(
            settings = rig.settings,
            notifications = if (rig.withCard) listOf(card) else emptyList(),
            selectedIndex = 0,
            expanded = false,
            cardDepth = CardDepth.First,
            mediaExpansion = MediaExpansion(key = null, index = 0, onChange = { _, _ -> }),
            readingExpandedText = false,
            siblingPickerOpen = false,
            hasIdleStoredCards = false
        )
        return IslandWiring(mutableStateOf(actions), mutableStateOf(view))
    }

    private fun mount(
        settings: SmartIslandSettings,
        withCard: Boolean = true,
        dragExpandAccepts: Boolean = false,
        glyphHolds: Boolean = false,
        landscape: Boolean = false
    ): Rig {
        val r = Rig(settings, withCard, dragExpandAccepts, glyphHolds, landscape)
        rig = r
        compose.setContent {
            val base = LocalContext.current
            val context = remember { VibratorLookups(base).also { r.vibrator = it } }
            val scope = rememberCoroutineScope()
            val wiring = remember { wiringFor(r) }
            val runner = remember { IslandActionRunner(context, wiring) }
            val pickerLeft = remember { mutableStateOf(0f) }
            val pickerOnLeft = remember { mutableStateOf(false) }
            Box(
                Modifier
                    .testTag(SHELL)
                    .size(300.dp, 40.dp)
                    .islandMainGestures(
                        settings = r.settings,
                        isInputActive = false,
                        isLandscape = r.landscape,
                        wiring = wiring,
                        ui = r.ui,
                        runner = runner,
                        scope = scope,
                        context = context,
                        pickerLeftFraction = pickerLeft,
                        pickerOnLeft = pickerOnLeft,
                        // a hold on the glyph of a pill with content is the glyph hold, the other tests
                        // keep the long-press path
                        pillHasContent = r.glyphHolds
                    )
            )
        }
        return r
    }

    // the touch lands on the pill, left of the camera band, so a tap is the card toggle
    private fun press() {
        compose.onNodeWithTag(SHELL).performTouchInput { down(Offset(100.dp.toPx(), 17.dp.toPx())) }
    }

    private fun pressPastHoldThreshold() {
        press()
        advanceClock(hold + 50)
    }

    private fun pressAt(xDp: Float) {
        compose.onNodeWithTag(SHELL).performTouchInput { down(Offset(xDp.dp.toPx(), 17.dp.toPx())) }
    }

    private fun moveBy(dxDp: Float, dyDp: Float) {
        compose.onNodeWithTag(SHELL).performTouchInput { moveBy(Offset(dxDp.dp.toPx(), dyDp.dp.toPx())) }
    }

    private fun release() {
        compose.onNodeWithTag(SHELL).performTouchInput { up() }
    }

    private fun cancelTouch() {
        compose.onNodeWithTag(SHELL).performTouchInput { cancel() }
    }

    private fun advanceClock(ms: Long) = compose.mainClock.advanceTimeBy(ms, ignoreFrameDuration = true)

    private fun buzzes() = rig.vibrator?.count ?: 0

    private fun assertPillAtRest() {
        assertEquals(0f, rig.ui.pillArm.value, 0f)
        assertEquals(0f, rig.ui.deadZone.value, 0f)
        assertEquals(0f, rig.ui.pillContentDragX, 0f)
        assertFalse(rig.ui.pillOpen.active)
        assertFalse(rig.ui.pillTouchActive)
    }

    @Test
    fun holdReachesTheThresholdWithoutReleaseAndRunsOnce() {
        val r = mount(toggleHold)
        press()
        advanceClock(hold - 50)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, buzzes())

        advanceClock(100)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(1, buzzes())

        advanceClock(2_000)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(1, buzzes())
        assertEquals(0, r.counts.toggle)
        assertEquals(0, r.counts.openApps)
    }

    @Test
    fun releaseAfterTheHoldDoesNotTapOrRepeat() {
        val r = mount(toggleHold)
        pressPastHoldThreshold()
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(1, buzzes())
        assertEquals(0, r.counts.toggle)
        assertEquals(0, r.counts.other)
        assertPillAtRest()
    }

    @Test
    fun shortTapTogglesTheCardOnceAndNeverHolds() {
        val r = mount(toggleHold)
        press()
        advanceClock(50)
        release()
        advanceClock(2_000)
        assertEquals(1, r.counts.toggle)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, buzzes())
        assertEquals(0, r.counts.openApps)
        assertPillAtRest()
    }

    @Test
    fun cancelledTouchNeverReachesTheThreshold() {
        val r = mount(toggleHold)
        press()
        advanceClock(100)
        cancelTouch()
        // let the cancel reach the gesture handler before the clock jumps past the hold threshold
        compose.waitForIdle()
        advanceClock(50)
        advanceClock(2_000)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, buzzes())
        assertFalse(r.ui.pillTouchActive)
    }

    @Test
    fun movementPastTheSlopBlocksTheActionAndTheBuzz() {
        val r = mount(toggleHold.copy(enablePillSwipeActions = false))
        press()
        advanceClock(50)
        moveBy(40f, 0f)
        advanceClock(hold + 50)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, buzzes())

        release()
        advanceClock(1_000)
        assertEquals(0, r.counts.toggle)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, buzzes())
        assertPillAtRest()
    }

    @Test
    fun appsShortcutArmsSilentlyAndOpensOnceOnRelease() {
        val r = mount(appsHold, withCard = false)
        pressPastHoldThreshold()
        assertEquals(0, r.counts.openApps)
        assertEquals(0, buzzes())
        assertEquals(0, r.counts.togglePillHidden)

        advanceClock(1_000)
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.openApps)
        assertEquals(0, buzzes())
        assertEquals(0, r.counts.toggle)
        assertPillAtRest()
    }

    @Test
    fun appsShortcutDoesNotOpenWhenTheFingerSlidAfterArming() {
        val r = mount(appsHold, withCard = false)
        pressPastHoldThreshold()
        moveBy(40f, 0f)
        release()
        advanceClock(1_000)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.toggle)
    }

    @Test
    fun dragAfterTheHoldFiresNoSwipeAction() {
        val r = mount(toggleHold)
        pressPastHoldThreshold()
        moveBy(120f, 0f)
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(1, buzzes())
        assertEquals(0, r.counts.toggle)
        assertEquals(0, r.counts.other)
    }

    @Test
    fun verticalDragThatOpensTheIslandEndsTheHold() {
        val r = mount(toggleHold, dragExpandAccepts = true)
        press()
        advanceClock(50)
        moveBy(0f, 30f)
        advanceClock(hold + 200)
        assertEquals(1, r.counts.dragExpand)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, buzzes())

        release()
        advanceClock(1_000)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
    }

    @Test
    fun aTouchAfterAHoldStartsClean() {
        val r = mount(toggleHold)
        pressPastHoldThreshold()
        release()
        advanceClock(500)

        press()
        advanceClock(50)
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(1, r.counts.toggle)
        assertEquals(1, buzzes())
        assertPillAtRest()
    }

    private companion object {
        const val SHELL = "shell"
    }

    // the touch of press() lands left of the camera band: the glyph

    @Test
    fun glyphHoldBuzzesAndGlowsOnceAndRunsNoAction() {
        val r = mount(appsHold, glyphHolds = true)
        press()
        advanceClock(hold - 50)
        assertEquals(0, buzzes())
        assertEquals(0f, r.ui.holdGlow.value, 0f)

        advanceClock(100)
        assertEquals(1, buzzes())
        advanceClock(500)
        assertTrue(r.ui.holdGlow.value > 0f)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, r.counts.repulse)
    }

    @Test
    fun glyphHoldThenLiftOpensTheAppsShortcutOnce() {
        val r = mount(appsHold, glyphHolds = true)
        pressPastHoldThreshold()
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.openApps)
        assertEquals(0, r.counts.repulse)
        assertEquals(1, buzzes())
        assertEquals(0f, r.ui.holdGlow.value, 0f)
        assertPillAtRest()
    }

    @Test
    fun glyphHoldThenSwipeLeftOrRightRepulsesTheCurrentCard() {
        for (dx in listOf(-60f, 60f)) {
            val r = mount(appsHold, glyphHolds = true)
            pressPastHoldThreshold()
            moveBy(dx, 0f)
            release()
            advanceClock(1_000)
            assertEquals("swipe $dx", 1, r.counts.repulse)
            assertEquals(0, r.counts.openApps)
            assertEquals(0, r.counts.togglePillHidden)
            assertEquals(0f, r.ui.holdGlow.value, 0f)
            assertPillAtRest()
            compose.runOnIdle { }
        }
    }

    @Test
    fun glyphHoldThenSwipePulledBackToTheMiddleDoesNothing() {
        val r = mount(appsHold, glyphHolds = true)
        pressPastHoldThreshold()
        moveBy(60f, 0f)
        moveBy(-60f, 0f)
        release()
        advanceClock(1_000)
        assertEquals(0, r.counts.repulse)
        assertEquals(0, r.counts.openApps)
    }

    @Test
    fun glyphHoldThenVerticalDragDoesNothing() {
        val r = mount(appsHold, glyphHolds = true)
        pressPastHoldThreshold()
        moveBy(0f, 40f)
        release()
        advanceClock(1_000)
        assertEquals(0, r.counts.repulse)
        assertEquals(0, r.counts.openApps)
    }

    @Test
    fun cancelledGlyphHoldLeavesNoGlow() {
        val r = mount(appsHold, glyphHolds = true)
        pressPastHoldThreshold()
        cancelTouch()
        compose.waitForIdle()
        advanceClock(1_000)
        assertEquals(0f, r.ui.holdGlow.value, 0f)
        assertEquals(0, r.counts.repulse)
    }

    @Test
    fun centreBandLongPress() {
        val r = mount(toggleHold, glyphHolds = true)
        compose.onNodeWithTag(SHELL).performTouchInput { down(Offset(150.dp.toPx(), 17.dp.toPx())) }
        advanceClock(hold + 50)
        assertEquals(1, r.counts.togglePillHidden)
        assertEquals(0f, r.ui.holdGlow.value, 0f)
        assertEquals(0, r.counts.repulse)
    }

    @Test
    fun landscapeGlyphHoldBuzzesGlowsAndRunsNoAction() {
        val r = mount(appsHold, glyphHolds = true, landscape = true)
        pressPastHoldThreshold()
        assertEquals(1, buzzes())
        advanceClock(500)
        assertTrue(r.ui.holdGlow.value > 0f)
        assertEquals(0, r.counts.togglePillHidden)
        assertEquals(0, r.counts.openApps)
        assertEquals(0, r.counts.repulse)
    }

    @Test
    fun landscapeGlyphHoldThenLiftOpensTheAppsShortcutOnce() {
        val r = mount(appsHold, glyphHolds = true, landscape = true)
        pressPastHoldThreshold()
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.openApps)
        assertEquals(0, r.counts.repulse)
        assertEquals(0f, r.ui.holdGlow.value, 0f)
        assertPillAtRest()
    }

    @Test
    fun landscapeHoldSwipeRepulse() {
        for (dx in listOf(-60f, 60f)) {
            val r = mount(appsHold, glyphHolds = true, landscape = true)
            pressPastHoldThreshold()
            moveBy(dx, 0f)
            release()
            advanceClock(1_000)
            assertEquals("swipe $dx", 1, r.counts.repulse)
            assertEquals(0, r.counts.openApps)
            assertEquals(0f, r.ui.holdGlow.value, 0f)
            assertPillAtRest()
            compose.runOnIdle { }
        }
    }

    @Test
    fun landscapeHoldOffTheGlyphKeepsTheSilentAppsShortcut() {
        val r = mount(appsHold, glyphHolds = true, landscape = true)
        pressAt(250f)
        advanceClock(hold + 50)
        assertEquals(0, buzzes())
        assertEquals(0f, r.ui.holdGlow.value, 0f)
        release()
        advanceClock(1_000)
        assertEquals(1, r.counts.openApps)
        assertEquals(0, r.counts.repulse)
    }
}
