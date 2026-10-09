package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDataTest {

    @Test fun everyCategoryHasAtLeastOneNgo() {
        FoodCategory.entries.forEach { category ->
            assertTrue("No NGO accepts ${category.label}", ngosAccepting(category).isNotEmpty())
        }
    }

    @Test fun filteringOnlyReturnsNgosThatAcceptTheCategory() {
        FoodCategory.entries.forEach { category ->
            assertTrue(ngosAccepting(category).all { category in it.accepts })
        }
    }

    @Test fun noFilterReturnsEveryNgoWithUrgentFirstThenNearest() {
        val all = ngosAccepting(null)
        assertEquals(sampleNgoRequests.size, all.size)
        assertTrue(all.first().urgent)
        val others = all.filter { !it.urgent }.map { it.distanceKm }
        assertEquals(others.sorted(), others)
    }

    @Test fun pledgeProgressStaysBetweenZeroAndOne() {
        sampleNgoRequests.forEach { assertTrue(it.progress in 0f..1f) }
        val first = sampleNgoRequests.first()
        assertEquals(0f, first.copy(goal = 0).progress, 0f)
        assertEquals(1f, first.copy(pledged = first.goal * 2).progress, 0f)
    }

    @Test fun communityProgressIsClamped() {
        assertEquals(0.832f, communityProgress(), 0.001f)
        assertEquals(1f, communityProgress(shared = 20_000, goal = 15_000), 0f)
        assertEquals(0f, communityProgress(shared = 10, goal = 0), 0f)
    }

    @Test fun pickupLabelFallsBackToTheDefaultArea() {
        assertEquals(DEFAULT_PICKUP_AREA, pickupLabel(""))
        assertEquals(DEFAULT_PICKUP_AREA, pickupLabel("   "))
        assertEquals("Gotri, Vadodara", pickupLabel("  Gotri, Vadodara "))
    }

    @Test fun timeAgoUsesTheRightUnit() {
        val now = 1_000_000_000_000L
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        assertEquals("Just now", timeAgoLabel(now - 30_000L, now))
        assertEquals("Just now", timeAgoLabel(now + hour, now))
        assertEquals("5 min ago", timeAgoLabel(now - 5 * minute, now))
        assertEquals("1 hour ago", timeAgoLabel(now - hour, now))
        assertEquals("3 hours ago", timeAgoLabel(now - 3 * hour, now))
        assertEquals("Yesterday", timeAgoLabel(now - 26 * hour, now))
        assertEquals("3 days ago", timeAgoLabel(now - 3 * day, now))
        assertEquals("1 week ago", timeAgoLabel(now - 9 * day, now))
        assertEquals("2 weeks ago", timeAgoLabel(now - 15 * day, now))
    }

    @Test fun theDonationOnItsWayHasAVolunteerAndAnEta() {
        val donation = recentDonations.first { it.inTransit }
        assertNotNull(donation.volunteer)
        assertNotNull(donation.etaMinutes)
    }

    @Test fun deliveredDonationsMatchWhatTheNgoSideReceived() {
        val receivedTitles = sampleReceivals.map { it.title }
        recentDonations.filter { !it.inTransit }.forEach { assertTrue(it.title in receivedTitles) }
    }

    @Test fun categoryIsGuessedFromTheDishName() {
        assertEquals(FoodCategory.Meals, guessCategory("Veg Biryani, 12 servings"))
        assertEquals(FoodCategory.Bakery, guessCategory("Bakery Surplus, 20 breads"))
        assertEquals(FoodCategory.Vegetables, guessCategory("Fresh Vegetable Basket"))
        assertEquals(FoodCategory.Fruit, guessCategory("Mixed fruit tray"))
        assertEquals(FoodCategory.Dairy, guessCategory("Paneer and curd"))
        assertEquals(FoodCategory.Packaged, guessCategory("Sealed biscuit packets"))
        assertEquals(FoodCategory.Meals, guessCategory("Something unusual"))
    }

    @Test fun aNewDonationCountsAsInTransitUntilDelivered() {
        val posted = DonationItem("Dal khichdi", "Nearby NGOs", "Just now", STATUS_POSTED, 200, FoodCategory.Meals, stage = 0)
        assertTrue(posted.inTransit)
        assertTrue(!posted.copy(status = STATUS_DELIVERED).inTransit)
    }
}
