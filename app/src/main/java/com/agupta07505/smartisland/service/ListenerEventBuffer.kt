/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * The bounded queue between the notification listener's callbacks and the repository (Rule_Book 5): the
 * equivalent of `.buffer(capacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)`, made safe for a
 * stream that mixes posts and removals. One consumer drains it in order, so events for a key are handled
 * one at a time and in the order they happened. Pure (no Android), so it is unit-testable.
 *
 *  - A post for a key whose newest queued event is also a post replaces that post where it stands: only the
 *    latest content matters, and the key keeps its place in the arrival order.
 *  - A removal is queued after a pending post rather than replacing it, because the removal may turn out to be
 *    a no-op (the listener cancelling a notification itself) and the post must then still land. A second
 *    removal for the same key adds nothing.
 *  - Past [capacity] queued events, the oldest *post* is dropped. A removal is never dropped: dropping one could
 *    leave an alert on the island that the app has already withdrawn. Removals are only ever one per key, so
 *    the queue stays bounded by the number of distinct keys even in a burst of them.
 */
class ListenerEventBuffer<E : Any>(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val keyOf: (E) -> String,
    private val isRemoval: (E) -> Boolean
) {
    private val queue = ArrayList<E>()
    private val lock = Any()
    private val signal = Channel<Unit>(Channel.CONFLATED)

    /** Posts dropped on overflow since creation; for logs and tests. */
    var droppedCount: Int = 0
        private set

    val size: Int get() = synchronized(lock) { queue.size }

    /** Queues [event]; never blocks, safe from any thread. */
    fun offer(event: E) {
        synchronized(lock) {
            val key = keyOf(event)
            val lastIndex = queue.indexOfLast { keyOf(it) == key }
            val last = if (lastIndex >= 0) queue[lastIndex] else null
            when {
                last == null -> queue.add(event)
                isRemoval(event) && isRemoval(last) -> Unit
                !isRemoval(event) && !isRemoval(last) -> queue[lastIndex] = event
                else -> queue.add(event)
            }
            while (queue.size > capacity) {
                val oldestPost = queue.indexOfFirst { !isRemoval(it) }
                if (oldestPost < 0) break
                queue.removeAt(oldestPost)
                droppedCount++
            }
        }
        signal.trySend(Unit)
    }

    /** Takes the oldest queued event, or null when the queue is empty. */
    fun poll(): E? = synchronized(lock) { if (queue.isEmpty()) null else queue.removeAt(0) }

    fun clear() = synchronized(lock) { queue.clear() }

    /** Starts the single consumer: it handles every queued event in order, waiting when the queue runs dry. */
    fun consumeIn(scope: CoroutineScope, handle: suspend (E) -> Unit): Job = scope.launch {
        for (unit in signal) {
            while (true) {
                val event = poll() ?: break
                handle(event)
            }
        }
    }

    companion object {
        const val DEFAULT_CAPACITY = 10
    }
}
