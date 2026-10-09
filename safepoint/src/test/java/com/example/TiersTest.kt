package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TiersTest {
    @Test fun startsInBronzeWithProgressToSilver() {
        val status = tierStatus(1240)
        assertEquals("Bronze", status.current)
        assertEquals("Silver", status.next)
        assertEquals(760, status.pointsToNext)
        assertEquals(0.62f, status.progress, 0.001f)
    }

    @Test fun movesUpAtTheThreshold() {
        assertEquals("Silver", tierStatus(2000).current)
        assertEquals("Gold", tierStatus(5000).current)
        assertEquals("Platinum", tierStatus(10000).current)
    }

    @Test fun topTierHasNoNextTier() {
        val status = tierStatus(25000)
        assertNull(status.next)
        assertEquals(1f, status.progress, 0f)
    }
}
