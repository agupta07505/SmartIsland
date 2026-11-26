/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * The island's window view. While the window is focusable (see BackStepRules.windowFocusable) the system sends it Back,
 * the key of a 3-button bar as much as the back gesture, and [onBack] says whether the island took it
 */
@Suppress("ViewConstructor")
internal class OverlayComposeView(
    context: Context,
    private val onBack: () -> Boolean
) : AbstractComposeView(context) {
    // ComposeView is final, so this is its own body: the same content slot over AbstractComposeView
    private var hostedContent by mutableStateOf<(@Composable () -> Unit)?>(null)

    @Composable
    override fun Content() {
        hostedContent?.invoke()
    }

    // like ComposeView, the composition starts once content is set
    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        hostedContent = content
        if (isAttachedToWindow) createComposition()
    }

    override fun getAccessibilityClassName(): CharSequence = javaClass.name

    /** Whether a fullscreen app hides the island: its touches go to [hiddenTouch] and never reach the Compose content. */
    var hiddenNow: () -> Boolean = { false }
    var hiddenTouch: ((MotionEvent) -> Unit)? = null

    /** Decided once per gesture at its first touch, a gesture that began on the island is not cut in the middle. */
    private var swallowing = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) swallowing = hiddenTouch != null && hiddenNow()
        if (swallowing) {
            hiddenTouch?.invoke(ev)
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                swallowing = false
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    /** The press of Back that went to the island, its repeats and its release go there too. */
    private var backTaken = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) backTaken = onBack()
            if (backTaken) {
                if (event.action == KeyEvent.ACTION_UP) backTaken = false
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
