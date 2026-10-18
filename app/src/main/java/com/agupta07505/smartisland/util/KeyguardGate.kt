/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.agupta07505.smartisland.ui.UnlockTrampolineActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * What an island control does, for the lockscreen gate (Rule_Book §8.3). Sensitive actions (they reveal or
 * act on a message, or leave the lockscreen for an app) need the phone unlocked; safe ones (play/pause, stop
 * a timer) work from the lockscreen.
 */
enum class GatedAction(val needsUnlock: Boolean) {
    Reply(true),
    MarkAsRead(true),
    OpenApp(true),
    AppAction(true),
    MediaControl(false),
    TimerControl(false);

    /** Whether this action must wait for the keyguard to be dismissed when the phone is [isLocked]. */
    fun requiresUnlock(isLocked: Boolean): Boolean = isLocked && needsUnlock
}

/**
 * Holds at most one action deferred until the keyguard is dismissed: a newer request replaces an older one,
 * a successful dismissal hands it out once, a cancelled or failed one drops it. Pure, so it is unit-testable.
 */
class PendingActionGate<T : Any> {
    private var pending: T? = null

    val hasPending: Boolean get() = pending != null

    fun defer(action: T) {
        pending = action
    }

    /** The cached action, to run now that the user authenticated; null when none was waiting. */
    fun onDismissSucceeded(): T? = pending.also { pending = null }

    fun onDismissFailed() {
        pending = null
    }
}

/** Which alert's reply field should open once the island reopens after an unlock; consumed once. Pure. */
class PendingReply {
    @Volatile private var key: String? = null

    fun request(key: String) {
        this.key = key
    }

    fun consume(key: String?): Boolean {
        if (key == null || this.key != key) return false
        this.key = null
        return true
    }
}

/**
 * Lockscreen action gate (Rule_Book §8.3). While the keyguard is locked a sensitive action is not run: the
 * island collapses, [UnlockTrampolineActivity] calls `KeyguardManager.requestDismissKeyguard`, and the action
 * runs only from `onDismissSucceeded`. Safe actions run straight away.
 */
object KeyguardGate {
    private const val TAG = "KeyguardGate"

    private val gate = PendingActionGate<() -> Unit>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val _replyRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Keys whose inline reply field should open again once the island is unlocked (see [requestReply]). */
    val replyRequests: SharedFlow<String> = _replyRequests.asSharedFlow()

    private val pendingReply = PendingReply()

    fun isLocked(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    /**
     * Runs [block] now, or, while the phone is locked and [action] is sensitive, collapses the island with
     * [onCollapse], asks the user to unlock and runs [block] only after a successful unlock.
     */
    fun run(context: Context, action: GatedAction, onCollapse: () -> Unit = {}, block: () -> Unit) {
        if (!action.requiresUnlock(isLocked(context))) {
            block()
            return
        }
        synchronized(gate) { gate.defer(block) }
        runCatchingLogged(TAG, "Collapse before unlock failed") { onCollapse() }
        val launched = runCatchingLogged(TAG, "Unlock trampoline launch failed") {
            context.startActivity(
                Intent(context, UnlockTrampolineActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            )
        }
        if (launched == null) onDismissFailed()
    }

    /** The user authenticated: run the cached action on the main thread. */
    fun onDismissSucceeded() {
        val action = synchronized(gate) { gate.onDismissSucceeded() } ?: return
        val runIt = { runCatchingLogged(TAG, "Deferred island action failed") { action() } }
        if (Looper.myLooper() == Looper.getMainLooper()) runIt() else mainHandler.post { runIt() }
    }

    fun onDismissFailed() {
        synchronized(gate) { gate.onDismissFailed() }
    }

    /** Asks the island to reopen [key]'s card with the reply field open (the deferred half of "Reply"). */
    fun requestReply(key: String) {
        pendingReply.request(key)
        _replyRequests.tryEmit(key)
    }

    /** True once for [key] after [requestReply]: the card opens straight into its reply field. */
    fun consumeReplyRequest(key: String?): Boolean = pendingReply.consume(key)
}
