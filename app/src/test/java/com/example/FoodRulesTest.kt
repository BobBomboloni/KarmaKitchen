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

    @Test fun servingsButtonsStepByOneAndStayInRange() {
        assertEquals("5 servings", adjustServings("4-6 servings", 1))
        assertEquals("1 serving", adjustServings("", 1))
        assertEquals("1 serving", adjustServings("1 serving", -1))
        assertEquals("99 servings", adjustServings("99 servings", 1))
        assertEquals(12, servingsCount("12 servings"))
    }

    @Test fun perServingCoinsMatchTheTotal() {
        assertEquals(50, karmaPerServing("Fresh"))
        assertEquals(30, karmaPerServing("Good"))
        assertEquals(20, karmaPerServing("Consume today"))
        assertEquals(countedServings("8 servings") * karmaPerServing("Fresh"), calculateKarmaPoints("8 servings", "Fresh"))
    }
}
