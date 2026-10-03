package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodRulesTest {
    @Test fun spoiledQualitiesAreUnsafe() {
        assertTrue(isUnsafeQuality("Spoiled/Unsafe"))
        assertTrue(isUnsafeQuality("Visible mould on surface"))
        assertTrue(isUnsafeQuality("Rotten"))
        assertTrue(isUnsafeQuality("Expired"))
    }

    @Test fun carrotAndRotisserieAreNotFlagged() {
        assertFalse(isUnsafeQuality("Fresh carrot sticks"))
        assertFalse(isUnsafeQuality("Fresh rotisserie chicken"))
    }

    @Test fun freshQualityIsSafe() {
        assertFalse(isUnsafeQuality("Fresh & Excellent"))
    }

    @Test fun pointsUseServingsTimesMultiplier() {
        assertEquals(4 * 50, calculateKarmaPoints("4-6 servings", "Fresh & Excellent"))
        assertEquals(3 * 30, calculateKarmaPoints("3 servings", "Good Quality"))
        assertEquals(2 * 20, calculateKarmaPoints("2 plates", "Consume within today"))
    }

    @Test fun pointsAreCappedAndDefaultToOneServing() {
        assertEquals(MAX_SERVINGS_FOR_POINTS * 50, calculateKarmaPoints("500 servings", "Fresh"))
        assertEquals(50, calculateKarmaPoints("a bowl", "Fresh"))
    }
}
