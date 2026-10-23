/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedAppsTest {
    @Test fun moveShiftsTheOthersAlong() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), PinnedApps.move(list, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), PinnedApps.move(list, 3, 0))
    }

    @Test fun moveOutOfRangeOrOntoItselfChangesNothing() {
        val list = listOf("a", "b", "c")
        assertEquals(list, PinnedApps.move(list, 1, 1))
        assertEquals(list, PinnedApps.move(list, -1, 2))
        assertEquals(list, PinnedApps.move(list, 0, 3))
    }

    @Test fun moveOntoPutsTheAppInTheTargetsPlace() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "d", "a"), PinnedApps.moveOnto(list, "a", "d"))
        assertEquals(listOf("c", "a", "b", "d"), PinnedApps.moveOnto(list, "c", "a"))
        assertEquals(list, PinnedApps.moveOnto(list, "a", "missing"))
    }

    @Test fun swapTradesTwoPlacesAndLeavesTheRest() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "b", "a", "d"), PinnedApps.swap(list, "a", "c"))
        assertEquals(listOf("a", "b", "d", "c"), PinnedApps.swap(list, "d", "c"))
        assertEquals(list, PinnedApps.swap(list, "a", "a"))
        assertEquals(list, PinnedApps.swap(list, "a", "missing"))
        // A folder trades places like any other entry.
        assertEquals(listOf(f("x", "y"), "b"), PinnedApps.swap(listOf("b", f("x", "y")), "b", f("x", "y")))
    }

    @Test fun encodeDecodeKeepsTheOrder() {
        val list = listOf("com.z.app", "com.a.app", "com.m.app")
        assertEquals(list, PinnedApps.decode(PinnedApps.encode(list)))
    }

    @Test fun normalizeDropsBlankDuplicateAndExtraEntries() {
        val many = (1..12).map { "pkg$it" }
        assertEquals(many.take(PinnedApps.MAX), PinnedApps.normalize(many))
        assertEquals(listOf("a", "b"), PinnedApps.normalize(listOf("a", " ", "b", "a", "")))
        assertEquals(emptyList<String>(), PinnedApps.decode(""))
        assertEquals(emptyList<String>(), PinnedApps.decode(null))
    }

    @Test fun legacySetIsUsedOnlyUntilAnOrderIsSaved() {
        val legacy = linkedSetOf("x", "y")
        assertEquals(listOf("x", "y"), PinnedApps.restore(null, legacy))
        assertEquals(listOf("y", "x"), PinnedApps.restore("y\nx", legacy))
        // An empty saved order means the user unpinned everything; the old set must not come back.
        assertEquals(emptyList<String>(), PinnedApps.restore("", legacy))
        assertEquals(emptyList<String>(), PinnedApps.restore(null, null))
    }

    private fun f(vararg pkgs: String) = "folder:" + pkgs.joinToString(",")

    @Test fun joiningTwoAppsMakesAFolderInTheTargetsPlaceAndFreesOneSlot() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", f("b", "d"), "c"), PinnedApps.joinFolder(list, "d", "b"))
        assertEquals(listOf(f("a", "c"), "b", "d"), PinnedApps.joinFolder(list, "c", "a"))
    }

    @Test fun anAppJoinsAFolderUntilItHoldsFour() {
        val list = listOf("a", f("b", "c", "d"), "e")
        assertEquals(listOf("a", f("b", "c", "d", "e")), PinnedApps.joinFolder(list, "e", f("b", "c", "d")))
        val full = listOf("a", f("b", "c", "d", "e"))
        assertEquals(full, PinnedApps.joinFolder(full, "a", f("b", "c", "d", "e")))
    }

    @Test fun foldersDoNotNestOrJoinThemselves() {
        val list = listOf(f("a", "b"), f("c", "d"), "e")
        assertEquals(list, PinnedApps.joinFolder(list, f("a", "b"), f("c", "d")))
        assertEquals(list, PinnedApps.joinFolder(list, "e", "e"))
        assertEquals(list, PinnedApps.joinFolder(list, "missing", "e"))
    }

    @Test fun aFolderCountsAsOneSlot() {
        val list = listOf(f("a", "b", "c", "d"), "e", "f", "g", "h", "i", "j", "k")
        assertEquals(8, PinnedApps.normalize(list).size)
        assertEquals(11, PinnedApps.flatPackages(list).size)
        // Eight slots are full, so no ninth app can be pinned, even though one of them is a folder.
        assertEquals(list, PinnedApps.pin(list, "z"))
    }

    @Test fun draggingOutOfAFolderGivesTheAppASlotAfterIt() {
        val list = listOf("a", f("b", "c", "d"), "e")
        assertEquals(listOf("a", f("b", "d"), "c", "e"), PinnedApps.dragOut(list, f("b", "c", "d"), "c"))
    }

    @Test fun draggingOutOfATwoAppFolderDissolvesIt() {
        val list = listOf("a", f("b", "c"), "e")
        assertEquals(listOf("a", "c", "b", "e"), PinnedApps.dragOut(list, f("b", "c"), "b"))
    }

    @Test fun draggingOutWithNoFreeSlotUnpinsTheApp() {
        val list = listOf(f("a", "b", "c"), "d", "e", "f", "g", "h", "i", "j")
        assertEquals(8, list.size)
        assertEquals(true, PinnedApps.dragOutUnpins(list))
        val out = PinnedApps.dragOut(list, f("a", "b", "c"), "b")
        assertEquals(listOf(f("a", "c"), "d", "e", "f", "g", "h", "i", "j"), out)
        assertEquals(false, "b" in PinnedApps.flatPackages(out))
        // Two apps left in the folder, one dragged out of a full grid: the other becomes a plain app and the dragged one goes.
        val tight = listOf(f("a", "b"), "d", "e", "f", "g", "h", "i", "j")
        assertEquals(listOf("a", "d", "e", "f", "g", "h", "i", "j"), PinnedApps.dragOut(tight, f("a", "b"), "b"))
    }

    @Test fun movingAnAppInsideAFolderReordersOnlyThatFolder() {
        val list = listOf("x", f("a", "b", "c"), "y")
        assertEquals(listOf("x", f("c", "a", "b"), "y"), PinnedApps.moveInFolder(list, f("a", "b", "c"), 2, 0))
        assertEquals(listOf("x", f("b", "c", "a"), "y"), PinnedApps.moveInFolder(list, f("a", "b", "c"), 0, 2))
        assertEquals(list, PinnedApps.moveInFolder(list, f("a", "b", "c"), 1, 1))
        assertEquals(list, PinnedApps.moveInFolder(list, f("a", "b", "c"), 0, 3))
        assertEquals(list, PinnedApps.moveInFolder(list, f("q", "r"), 0, 1))
        assertEquals(list, PinnedApps.moveInFolder(list, "x", 0, 0))
    }

    @Test fun unpinningFromAFolderShrinksOrDissolvesIt() {
        assertEquals(listOf(f("a", "c"), "x"), PinnedApps.unpin(listOf(f("a", "b", "c"), "x"), "b"))
        assertEquals(listOf("a", "x"), PinnedApps.unpin(listOf(f("a", "b"), "x"), "b"))
        assertEquals(listOf("x"), PinnedApps.unpin(listOf("a", "x"), "a"))
    }

    @Test fun normalizeRepairsFoldersFromBackups() {
        // One app left after de-duplication: the folder is dissolved. An app already pinned earlier is not repeated in a folder.
        assertEquals(listOf("a", "b"), PinnedApps.normalize(listOf("a", f("a", "b"))))
        assertEquals(listOf(f("a", "b", "c", "d")), PinnedApps.normalize(listOf(f("a", "b", "c", "d", "e"))))
        assertEquals(emptyList<String>(), PinnedApps.normalize(listOf("folder:")))
        assertEquals(listOf(f("x", "y")), PinnedApps.decode(PinnedApps.encode(listOf(f("x", "y")))))
    }
}
