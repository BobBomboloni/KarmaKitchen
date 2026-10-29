package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DeliveryTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    // 9 Oct 2026, 18:00 IST
    private val now = 1_791_549_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute

    private val alkapuri = Place(22.3110, 73.1700)
    private val fatehgunj = Place(22.3250, 73.1880)

    @Test fun distancesAreRoughlyRight() {
        assertEquals(2.42, distanceKm(alkapuri, fatehgunj), 0.01)
        assertEquals(0.0, distanceKm(alkapuri, alkapuri), 0.0001)
        // Mumbai to Vadodara is about 360 km in a straight line.
        assertEquals(360.7, distanceKm(Place(19.0760, 72.8777), Place(22.3072, 73.1812)), 0.5)
    }

    @Test fun distancesReadNaturally() {
        assertEquals("850 m", distanceText(0.84))
        assertEquals("50 m", distanceText(0.01))
        assertEquals("2.4 km", distanceText(2.42))
        assertEquals("14 km", distanceText(13.6))
    }

    @Test fun geohashMatchesTheStandard() {
        assertEquals("u4pruydqqvj", geohash(Place(57.64911, 10.40744), precision = 11))
        assertEquals("teunckbuv", geohash(Place(22.3072, 73.1812)))
        // Places close together share a prefix, which is what makes range queries work.
        assertEquals("teu", geohash(alkapuri).take(3))
        assertEquals("teu", geohash(fatehgunj).take(3))
    }

    @Test fun directionsUseTheSpotOrTheAddress() {
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=22.311,73.17", directionsUrl(alkapuri, "ignored"))
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=Race%20Course%2C%20Vadodara",
            directionsUrl(null, "Race Course, Vadodara")
        )
        assertNull(directionsUrl(null, "  "))
    }

    @Test fun whatsAppNumbersGetTheCountryCode() {
        assertEquals("919876543210", whatsAppNumber("98765 43210"))
        assertEquals("919876543210", whatsAppNumber("098765-43210"))
        assertEquals("919876543210", whatsAppNumber("+91 98765 43210"))
        assertEquals("447700900123", whatsAppNumber("+44 7700 900123"))
        assertNull(whatsAppNumber("12345"))
        assertEquals("https://wa.me/919876543210?text=Hi%20there", whatsAppUrl("9876543210", "Hi there"))
        assertEquals("https://wa.me/919876543210", whatsAppUrl("9876543210"))
        assertNull(whatsAppUrl(""))
    }

    @Test fun eatByComesFromTheShelfLife() {
        assertEquals(now + 6 * hour, eatByFor("6 hours", now))
        assertEquals(now + 48 * hour, eatByFor("Good for 2 days", now))
        // Unknown shelf life: 4 hours. Never more than a week, never less than an hour.
        assertEquals(now + 4 * hour, eatByFor("", now))
        assertEquals(now + 24 * 7 * hour, eatByFor("3 months", now))
        assertEquals(now + hour, eatByFor("0 hours", now))
    }

    @Test fun clockAndTimeLeftReadNaturally() {
        assertEquals("9:30 PM", clockText(now + 3 * hour + 30 * minute, now, ist))
        assertEquals("10 Oct, 9:30 AM", clockText(now + 15 * hour + 30 * minute, now, ist))
        assertEquals("45 min", timeLeftText(now + 45 * minute, now))
        assertEquals("2 h 10 min", timeLeftText(now + 130 * minute, now))
        assertEquals("3 h", timeLeftText(now + 3 * hour, now))
        assertEquals("3 days", timeLeftText(now + 72 * hour, now))
        assertEquals("0 min", timeLeftText(now - hour, now))
    }

    @Test fun arrivalCountsDownFromAcceptance() {
        assertEquals(20, minutesUntilArrival(now - 10 * minute, 30, now))
        assertEquals(0, minutesUntilArrival(now - 50 * minute, 30, now))
        assertEquals(30, minutesUntilArrival(null, 30, now))
        assertNull(minutesUntilArrival(now, null, now))
    }

    @Test fun theRunnerIsLateHalfAnHourAfterTheEstimate() {
        assertFalse(runnerIsLate(now - 50 * minute, 30, now))
        assertTrue(runnerIsLate(now - 61 * minute, 30, now))
        // Without an estimate, 30 minutes are assumed.
        assertTrue(runnerIsLate(now - 61 * minute, null, now))
        assertFalse(runnerIsLate(null, 30, now))
    }

    @Test fun suggestedEtaFallsBackWhenTheDistanceIsUnknown() {
        assertEquals(etaMinutes(2.0), suggestedEta(2.0))
        assertEquals(DEFAULT_ETA_MINUTES, suggestedEta(-1.0))
    }

    @Test fun sharedOffersSayWhatWhereAndUntilWhen() {
        val text = shareOfferText("Veg biryani", 12, "Alkapuri, Vadodara", now + 3 * hour, now)
        assertTrue(text.startsWith("Free food for an NGO: 12 servings of Veg biryani, good until"))
        assertTrue(text.contains("Pickup: Alkapuri, Vadodara."))
        assertEquals(
            "Free food for an NGO: Cake. Accept it in the KarmaKitchen app.",
            shareOfferText("Cake", 1, "", null, now)
        )
    }

    @Test fun donorSeesWhenNobodyTookItOrItExpired() {
        val posted = DonationItem(
            "Dal khichdi", "Nearby NGOs", "Just now", STATUS_POSTED, 200, FoodCategory.Meals, stage = 0,
            createdAtMs = now - 40 * minute, eatByMs = now + hour
        )
        assertTrue(posted.hasNoTakers(now))
        assertFalse(posted.copy(createdAtMs = now - 10 * minute).hasNoTakers(now))
        assertFalse(posted.copy(stage = 1, status = STATUS_ACCEPTED).hasNoTakers(now))
        assertFalse(posted.isExpired(now))
        assertTrue(posted.isExpired(now + 2 * hour))
        assertTrue(posted.inTransit)
        assertFalse(posted.copy(status = STATUS_CANCELLED).inTransit)
        assertFalse(posted.copy(status = STATUS_EXPIRED).inTransit)
    }
}
