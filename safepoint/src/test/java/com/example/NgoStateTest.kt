package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NgoStateTest {
    @Before fun startFresh() {
        NgoState.reset()
        DonationLog.clear()
    }

    private fun offer(id: String = "d1", fromDonor: Boolean = true) = NgoDonation(
        id = id, donor = "Rohan", title = "Dal khichdi", servings = 12, isVeg = true, shelfLife = "6 hours",
        distanceKm = 1.4, pickup = "As soon as possible", category = FoodCategory.Meals,
        stage = OfferStage.Offered, fromDonor = fromDonor
    )

    private fun logged(id: String = "d1") = DonationItem(
        "Dal khichdi", "Nearby NGOs", "Just now", STATUS_POSTED, 600, FoodCategory.Meals, stage = 0, id = id
    )

    @Test fun expiryLabelsReadNaturally() {
        assertEquals("Expires in 4 hours", expiryLabel(4))
        assertEquals("Expires in 1 hour", expiryLabel(1))
        assertEquals("Expires in 3 days", expiryLabel(72))
        assertEquals("Expires in 3 months", expiryLabel(24 * 90))
        assertEquals("Expired 2 hours ago", expiryLabel(-2))
        assertEquals("Expired 2 days ago", expiryLabel(-48))
    }

    @Test fun stockStatusFollowsTheTimeLeft() {
        assertEquals(StockStatus.Fresh, stockStatus(25))
        assertEquals(StockStatus.Soon, stockStatus(24))
        assertEquals(StockStatus.Soon, stockStatus(1))
        assertEquals(StockStatus.Expired, stockStatus(0))
        assertEquals(StockStatus.Expired, stockStatus(-5))
    }

    @Test fun expiryTextBecomesHours() {
        assertEquals(12, hoursFromExpiryText("Expires in 12 hours"))
        assertEquals(48, hoursFromExpiryText("Consume within 2 days"))
        assertEquals(24 * 7, hoursFromExpiryText("Good for 1 week"))
        assertEquals(6, hoursFromExpiryText("Eat soon"))
        assertEquals(9, hoursFromExpiryText("", fallback = 9))
    }

    @Test fun lastSevenDaysEndOnToday() {
        // Calendar.WEDNESDAY is 4.
        assertEquals(listOf("T", "F", "S", "S", "M", "T", "W"), lastSevenDayLetters(4))
        assertEquals("S", lastSevenDayLetters(1).last())
    }

    @Test fun pickupEstimateGrowsWithDistanceAndVolunteerIsStable() {
        assertTrue(etaMinutes(5.0) > etaMinutes(1.0))
        assertEquals(volunteerFor("abc"), volunteerFor("abc"))
        assertTrue(volunteerFor("abc") in volunteerNames)
    }

    @Test fun anOfferMovesThroughAcceptAndReceiveOnBothSides() {
        DonationLog.add(logged())
        NgoState.post(offer())
        assertTrue(NgoState.offers.any { it.id == "d1" })

        NgoState.accept("d1")
        assertTrue(NgoState.offers.none { it.id == "d1" })
        assertTrue(NgoState.onTheWay.any { it.id == "d1" })
        val onTheWay = DonationLog.submitted.first { it.id == "d1" }
        assertEquals(1, onTheWay.stage)
        assertEquals(NGO_NAME, onTheWay.ngo)
        assertNotNull(onTheWay.volunteer)
        assertNotNull(onTheWay.etaMinutes)

        val stockBefore = NgoState.stock.size
        NgoState.markReceived("d1", IntakeSummary(true, "Looks fresh", "Expires in 12 hours", "Keep warm", emptyList()))
        assertTrue(NgoState.onTheWay.none { it.id == "d1" })
        assertNotNull(NgoState.receival("d1"))
        assertEquals(stockBefore + 1, NgoState.stock.size)
        assertEquals(12, NgoState.stock.first().hoursLeft)
        assertEquals(354, NgoState.mealsToday)
        val delivered = DonationLog.submitted.first { it.id == "d1" }
        assertEquals(STATUS_DELIVERED, delivered.status)
        assertFalse(delivered.inTransit)
        // The donor's coins are queued for their wallet.
        assertEquals(listOf(600), DonationLog.pendingCredits.toList())
    }

    @Test fun deliveriesThatDidNotComeFromTheDonorGiveNoCoins() {
        NgoState.markReceived("incoming-1", null)
        assertTrue(DonationLog.pendingCredits.isEmpty())
        assertNotNull(NgoState.receival("incoming-1"))
    }

    @Test fun declinedOffersAreHiddenAndNothingHappensOutOfOrder() {
        NgoState.decline("offer-1")
        assertTrue(NgoState.offers.none { it.id == "offer-1" })
        // Receiving something that was never accepted does nothing.
        val stock = NgoState.stock.size
        NgoState.markReceived("offer-2", null)
        assertEquals(stock, NgoState.stock.size)
    }

    @Test fun broadcastingShowsAsAnUrgentRequestForTheDonorHome() {
        assertNull(NgoState.broadcastRequest())
        NgoState.broadcasting = true
        val request = NgoState.broadcastRequest()
        assertNotNull(request)
        assertTrue(request!!.urgent)
        assertEquals(NGO_NAME, request.name)
        assertTrue(FoodCategory.Meals in request.accepts)
        NgoState.needs = emptySet()
        assertNull(NgoState.broadcastRequest())
    }

    @Test fun stockCanBeHandedOutOrDisposed() {
        val served = NgoState.peopleServed
        val item = NgoState.stock.first { it.quantity.contains("40") }
        NgoState.removeStock(item.id, distributed = true)
        assertEquals(served + 40, NgoState.peopleServed)
        val expired = NgoState.stock.first { stockStatus(it.hoursLeft) == StockStatus.Expired }
        NgoState.removeStock(expired.id, distributed = false)
        assertEquals(served + 40, NgoState.peopleServed)
        assertTrue(NgoState.stock.none { it.id == expired.id })
    }

    @Test fun attentionAndCapacityComeFromStock() {
        assertEquals(2, NgoState.needAttention)
        assertEquals(84, NgoState.capacityPercent)
        assertTrue(NgoState.awaitingSmile.isNotEmpty())
    }

    @Test fun bothRolesGetTheirOwnBottomBar() {
        assertEquals(NgoTabs, tabsForRoute(Screen.NgoOffers.route))
        assertEquals(NgoTabs, tabsForRoute(Screen.SendSmile.route))
        assertEquals(BottomTabs, tabsForRoute(Screen.Store.route))
        assertEquals(BottomTabs, tabsForRoute(Screen.Tiers.route))
        assertNull(tabsForRoute(Screen.Welcome.route))
        assertNull(tabsForRoute(Screen.RoleSelection.route))
        assertEquals(Screen.NgoSmiles.route, highlightedTabRoute(Screen.SendSmile.route))
    }
}
