package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryDemoTest {
    @Before fun startFresh() {
        NgoState.reset()
        DonationLog.clear()
        DeliveryDemo.reset()
    }

    @Test fun relayAsksNgosThenRidersThenDispatches() {
        assertEquals(RelayPhase.AskingNgos, relayPhase(0))
        assertEquals(RelayPhase.AskingNgos, relayPhase(DEMO_NGO_WINDOW_S * 1000 - 1))
        assertEquals(RelayPhase.AskingRiders, relayPhase(DEMO_NGO_WINDOW_S * 1000))
        assertEquals(RelayPhase.Dispatched, relayPhase((DEMO_NGO_WINDOW_S + DEMO_RIDER_WAIT_S) * 1000))
        assertEquals(DEMO_NGO_WINDOW_S, relaySecondsLeft(0))
        assertEquals(DEMO_RIDER_WAIT_S, relaySecondsLeft(DEMO_NGO_WINDOW_S * 1000))
        assertEquals(0L, relaySecondsLeft(10 * 60_000L))
    }

    @Test fun tripRunsFasterThanRealTimeAndStopsAtTheEnd() {
        val tripMs = 8 * 60_000L / DEMO_TRIP_SPEED
        assertEquals(0f, tripProgress(0, 8), 0.0001f)
        assertEquals(0.5f, tripProgress(tripMs / 2, 8), 0.0001f)
        assertEquals(1f, tripProgress(tripMs * 3, 8), 0.0001f)
        assertEquals(8, minutesLeft(0f, 8))
        assertEquals(4, minutesLeft(0.5f, 8))
        assertEquals(1, minutesLeft(0.99f, 8))
        assertEquals(0, minutesLeft(1f, 8))
    }

    @Test fun handoverCodeIsFourDigitsAndStable() {
        listOf("a", "offer-1", java.util.UUID.randomUUID().toString()).forEach { id ->
            val code = handoverCode(id)
            assertEquals(4, code.length)
            assertTrue(code.all { it.isDigit() })
            assertEquals(code, handoverCode(id))
        }
    }

    @Test fun eatByLabelReadsNaturally() {
        assertEquals("2h 0m", eatByLabel(EAT_WITHIN_MS))
        assertEquals("1h 40m", eatByLabel(100 * 60_000L))
        assertEquals("25m", eatByLabel(25 * 60_000L))
        assertEquals("1m", eatByLabel(1_000L))
        assertEquals("Past its best", eatByLabel(0))
    }

    @Test fun pointAlongFollowsTheRoute() {
        val route = listOf(0f to 0f, 1f to 0f, 1f to 1f)
        assertEquals(0f to 0f, pointAlong(route, 0f))
        assertEquals(1f to 0f, pointAlong(route, 0.5f))
        assertEquals(1f to 1f, pointAlong(route, 1f))
        val half = pointAlong(route, 0.25f)
        assertEquals(0.5f, half.first, 0.0001f)
        assertEquals(0f, half.second, 0.0001f)
        assertEquals(demoRoute.last(), pointAlong(demoRoute, 1f))
        // The drawn part of the route keeps every corner already passed.
        assertEquals(listOf(0f to 0f, 1f to 0f, 1f to 0.5f), routeUpTo(route, 0.75f))
    }

    @Test fun dispatchingAKarmaRiderPutsTheDonationOnItsWayOnBothSides() {
        NgoState.post(
            NgoDonation(
                id = "d1", donor = "Rohan", title = "Dal khichdi", servings = 12, isVeg = true, shelfLife = "6 hours",
                distanceKm = 1.4, pickup = "As soon as possible", category = FoodCategory.Meals,
                stage = OfferStage.Offered, fromDonor = true
            )
        )
        DonationLog.add(DonationItem("Dal khichdi", "Nearby NGOs", "Just now", STATUS_POSTED, 600, FoodCategory.Meals, stage = 0, id = "d1"))

        DeliveryDemo.dispatch("d1", CarrierKind.KarmaRider)

        val rider = carrierFor("d1", CarrierKind.KarmaRider).name
        val ngoCopy = NgoState.donations.first { it.id == "d1" }
        assertEquals(OfferStage.OnTheWay, ngoCopy.stage)
        assertEquals(rider, ngoCopy.volunteer)
        val donorCopy = DonationLog.submitted.first { it.id == "d1" }
        assertEquals(1, donorCopy.stage)
        assertEquals(STATUS_ON_THE_WAY, donorCopy.status)
        assertEquals(NGO_NAME, donorCopy.ngo)
        assertEquals(CarrierKind.KarmaRider, DeliveryDemo.kindOf("d1"))

        // The NGO can still check it in, and the donor still gets the coins.
        NgoState.markReceived("d1", null)
        assertEquals(STATUS_DELIVERED, DonationLog.submitted.first { it.id == "d1" }.status)
        assertEquals(listOf(600), DonationLog.pendingCredits.toList())
    }

    @Test fun dispatchLeavesADonationAnNgoAlreadyAcceptedAlone() {
        NgoState.accept("offer-1")
        val before = NgoState.donations.first { it.id == "offer-1" }
        DeliveryDemo.dispatch("offer-1", CarrierKind.Courier)
        assertEquals(before, NgoState.donations.first { it.id == "offer-1" })
        assertEquals(CarrierKind.NgoRunner, DeliveryDemo.kindOf("offer-1"))
    }
}
