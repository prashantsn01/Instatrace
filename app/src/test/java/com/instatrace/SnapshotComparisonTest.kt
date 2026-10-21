package com.instatrace

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotComparisonTest {
    @Test fun detectsLostAndGainedFollowers() {
        val result = compareSnapshots(setOf("a", "b", "c"), setOf("a", "c", "d"), setOf("a", "b", "d"))
        assertEquals(setOf("b"), result.lost)
        assertEquals(setOf("d"), result.gained)
        assertEquals(setOf("b"), result.notFollowingBack)
    }

    @Test fun firstSnapshotHasNoFalseUnfollowerClaims() {
        val result = compareSnapshots(null, setOf("a"), setOf("a", "b"))
        assertEquals(emptySet<String>(), result.lost)
        assertEquals(emptySet<String>(), result.gained)
        assertEquals(setOf("b"), result.notFollowingBack)
    }

    @Test fun handlesNoChanges() {
        val result = compareSnapshots(setOf("a", "b"), setOf("a", "b"), setOf("a"))
        assertEquals(emptySet<String>(), result.lost)
        assertEquals(emptySet<String>(), result.gained)
        assertEquals(emptySet<String>(), result.notFollowingBack)
    }
}
