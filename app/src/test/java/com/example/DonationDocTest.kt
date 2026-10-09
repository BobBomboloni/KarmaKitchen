package com.example

import com.example.cloud.DonationDoc
import com.example.cloud.STATE_OFFERED
import com.example.cloud.STATE_ON_THE_WAY
import com.example.cloud.STATE_RECEIVED
import com.example.cloud.earnedCoins
import com.example.cloud.friendlyDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DonationDocTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    // 9 Oct 2026, 18:00 IST
    private val now = 1_791_549_000_000L

    private fun doc(status: String = STATE_OFFERED, coins: Int = 500) = DonationDoc(
        id = "d1",
        donorId = "donor1",
        donorName = "Meera",
        donorPhone = "98765 43210",
        title = "Veg biryani",
        servings = 12,
        isVeg = true,
        shelfLife = "6 hours",
        quality = "Fresh",
        category = FoodCategory.Meals,
        coins = coins,
        pickupWindow = "As soon as possible",
        address = "Alkapuri, Vadodara",
        status = status,
        createdAtMs = now - 5 * 60_000
    )

    @Test
    fun createMapRoundTrips() {
        val original = doc()
        val read = DonationDoc.fromMap("d1", original.toCreateMap() + mapOf("createdAt" to original.createdAtMs))
        assertEquals(original, read)
    }

    @Test
    fun createMapAlwaysStartsAsAnOpenOffer() {
        val map = doc(status = STATE_RECEIVED).toCreateMap()
        assertEquals(STATE_OFFERED, map["status"])
        assertNull(map["ngoId"])
        assertEquals(false, map["donorAcknowledged"])
        assertEquals("Meals", map["category"])
    }

    @Test
    fun missingRequiredFieldsAreSkipped() {
        assertNull(DonationDoc.fromMap("x", mapOf("title" to "Rice")))
        assertNull(DonationDoc.fromMap("x", mapOf("donorId" to "a")))
    }

    @Test
    fun unknownCategoryFallsBackToMeals() {
        val read = DonationDoc.fromMap("x", mapOf("donorId" to "a", "title" to "Rice", "category" to "Snacks", "coins" to 3L))!!
        assertEquals(FoodCategory.Meals, read.category)
        assertEquals(3, read.coins)
    }

    @Test
    fun donorSeesTheRightStage() {
        assertEquals(0, doc(STATE_OFFERED).toDonationItem(now, ist).stage)
        assertEquals(STATUS_POSTED, doc(STATE_OFFERED).toDonationItem(now, ist).status)
        assertEquals(STATUS_ON_THE_WAY, doc(STATE_ON_THE_WAY).toDonationItem(now, ist).status)
        val delivered = doc(STATE_RECEIVED).copy(ngoName = "Hope Shelter").toDonationItem(now, ist)
        assertEquals(STATUS_DELIVERED, delivered.status)
        assertEquals(2, delivered.stage)
        assertEquals("Hope Shelter", delivered.ngo)
        assertFalse(delivered.inTransit)
    }

    @Test
    fun ngoSeesPickupDetails() {
        val offer = doc().toNgoDonation(now, ist)
        assertEquals(OfferStage.Offered, offer.stage)
        assertEquals("Alkapuri, Vadodara", offer.address)
        assertEquals("98765 43210", offer.donorPhone)
        assertTrue(offer.fromDonor)
        assertEquals("Meera", donationPlace(offer))
        assertEquals(OfferStage.Received, doc(STATE_RECEIVED).toNgoDonation(now, ist).stage)
    }

    @Test
    fun onlyReceivedDonationsEarnCoins() {
        val docs = listOf(
            doc(STATE_RECEIVED, coins = 500),
            doc(STATE_ON_THE_WAY, coins = 300),
            doc(STATE_OFFERED, coins = 200),
            doc(STATE_RECEIVED, coins = 150)
        )
        assertEquals(650, earnedCoins(docs))
    }

    @Test
    fun datesReadNaturally() {
        assertEquals("Just now", friendlyDate(now - 20_000, now, ist))
        assertEquals("Just now", friendlyDate(0L, now, ist))
        assertEquals("12 min ago", friendlyDate(now - 12 * 60_000, now, ist))
        assertEquals("Today, 1:30 PM", friendlyDate(now - (4 * 60 + 30) * 60_000L, now, ist))
        assertEquals("Yesterday, 7:45 PM", friendlyDate(now - (22 * 60 + 15) * 60_000L, now, ist))
        assertEquals("29 Sep", friendlyDate(now - 10 * 86_400_000L, now, ist))
    }

    @Test
    fun yesterdayWorksAcrossNewYear() {
        // 1 Jan 2027, 10:00 IST and 31 Dec 2026, 21:00 IST
        val newYear = 1_798_777_800_000L
        assertEquals("Yesterday, 9:00 PM", friendlyDate(newYear - 13 * 3_600_000L, newYear, ist))
    }
}
