/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The listener's bounded event queue (Rule_Book 5): ordered, capped at 10, and never drops a removal. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListenerEventBufferTest {

    private sealed interface Ev {
        val key: String
        data class Post(override val key: String, val version: Int = 0) : Ev
        data class Remove(override val key: String) : Ev
    }

    private fun buffer(capacity: Int = 10) =
        ListenerEventBuffer<Ev>(capacity = capacity, keyOf = { it.key }, isRemoval = { it is Ev.Remove })

    private fun ListenerEventBuffer<Ev>.drain(): List<Ev> = generateSequence { poll() }.toList()

    @Test
    fun eventsComeOutInArrivalOrder() {
        val b = buffer()
        b.offer(Ev.Post("a"))
        b.offer(Ev.Post("b"))
        b.offer(Ev.Remove("a"))
        assertEquals(listOf(Ev.Post("a"), Ev.Post("b"), Ev.Remove("a")), b.drain())
        assertNull(b.poll())
    }

    @Test
    fun aNewerPostForTheSameKeyReplacesTheQueuedOneInPlace() {
        val b = buffer()
        b.offer(Ev.Post("a", 1))
        b.offer(Ev.Post("b"))
        b.offer(Ev.Post("a", 2))
        assertEquals(listOf(Ev.Post("a", 2), Ev.Post("b")), b.drain())
    }

    @Test
    fun aRemovalNeverSwallowsThePostBeforeIt() {
        // The removal may be the listener's own cancel (a no-op); the post must still land first.
        val b = buffer()
        b.offer(Ev.Post("a"))
        b.offer(Ev.Remove("a"))
        b.offer(Ev.Remove("a"))
        b.offer(Ev.Post("a", 3))
        assertEquals(listOf(Ev.Post("a"), Ev.Remove("a"), Ev.Post("a", 3)), b.drain())
    }

    @Test
    fun aBurstDropsTheOldestPostsButKeepsEveryRemoval() {
        val b = buffer(capacity = 10)
        b.offer(Ev.Remove("gone"))
        repeat(15) { b.offer(Ev.Post("k$it")) }
        val out = b.drain()
        assertEquals(10, out.size)
        assertEquals(Ev.Remove("gone"), out.first())
        // The newest nine posts survive, in order.
        assertEquals((6 until 15).map { Ev.Post("k$it") }, out.drop(1))
        assertEquals(6, b.droppedCount)
    }

    @Test
    fun removalsAloneMayExceedTheCapacityRatherThanBeDropped() {
        val b = buffer(capacity = 3)
        repeat(5) { b.offer(Ev.Remove("k$it")) }
        assertEquals(5, b.drain().size)
        assertEquals(0, b.droppedCount)
    }

    @Test
    fun theConsumerHandlesEventsOneAtATimeInOrder() {
        val scope = TestScope(StandardTestDispatcher())
        val b = buffer()
        val handled = mutableListOf<Ev>()
        val job = b.consumeIn(scope) { handled += it }
        b.offer(Ev.Post("a"))
        b.offer(Ev.Remove("a"))
        scope.runCurrent()
        b.offer(Ev.Post("b"))
        scope.runCurrent()
        assertEquals(listOf(Ev.Post("a"), Ev.Remove("a"), Ev.Post("b")), handled)
        job.cancel()
    }
}
