package com.example

import com.example.cloud.DonationDoc
import com.example.cloud.STATE_ACCEPTED
import com.example.cloud.STATE_CANCELLED
import com.example.cloud.STATE_OFFERED
import com.example.cloud.STATE_ON_THE_WAY
import com.example.cloud.STATE_RECEIVED
import com.example.cloud.friendlyDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
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
        val original = doc().copy(
            donorPhone = "",
            place = Place(22.311, 73.17),
            eatByMs = now + 6 * 3_600_000L,
            deliveryType = DELIVERY_DROP_OFF,
            dropOffNgoId = "ngo1",
            dropOffNgoName = "Hope Shelter"
        )
        val read = DonationDoc.fromMap("d1", original.toCreateMap() + mapOf("createdAt" to original.createdAtMs))
        assertEquals(original, read)
    }

    @Test
    fun thePhoneNumberStaysOutOfTheDonation() {
        val map = doc().toCreateMap()
        assertFalse(map.containsKey("donorPhone"))
        // Without an eat-by time the field is left out, since the rules only accept a timestamp there.
        assertFalse(map.containsKey("eatBy"))
        assertEquals(Date(now), doc().copy(eatByMs = now).toCreateMap()["eatBy"])
        assertEquals(DELIVERY_PICKUP, map["deliveryType"])
        assertEquals("", map["geohash"])
        assertEquals("teuncj8fb", doc().copy(place = Place(22.311, 73.17)).toCreateMap()["geohash"])
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
        val accepted = doc(STATE_ACCEPTED).copy(ngoName = "Hope Shelter", volunteer = "Priya").toDonationItem(now, ist)
        assertEquals(STATUS_ACCEPTED, accepted.status)
        assertEquals(1, accepted.stage)
        assertEquals("Priya", accepted.volunteer)
        assertTrue(accepted.inTransit)
        assertEquals(STATUS_ON_THE_WAY, doc(STATE_ON_THE_WAY).toDonationItem(now, ist).status)
        assertEquals(2, doc(STATE_ON_THE_WAY).toDonationItem(now, ist).stage)
        val delivered = doc(STATE_RECEIVED).copy(ngoName = "Hope Shelter").toDonationItem(now, ist)
        assertEquals(STATUS_DELIVERED, delivered.status)
        assertEquals(3, delivered.stage)
        assertEquals("Hope Shelter", delivered.ngo)
        assertFalse(delivered.inTransit)
        assertEquals(STATUS_CANCELLED, doc(STATE_CANCELLED).toDonationItem(now, ist).status)
    }

    @Test
    fun anOfferPastItsEatByTimeHasExpired() {
        val stale = doc().copy(eatByMs = now - 60_000)
        assertTrue(stale.isExpired(now))
        assertEquals(STATUS_EXPIRED, stale.toDonationItem(now, ist).status)
        assertFalse(stale.toDonationItem(now, ist).inTransit)
        assertFalse(doc().copy(eatByMs = now + 60_000).isExpired(now))
        // Once accepted it is no longer an open offer, so it cannot expire.
        assertFalse(doc(STATE_ACCEPTED).copy(eatByMs = now - 60_000).isExpired(now))
        assertTrue(stale.toNgoDonation(now, ist).isExpired(now))
    }

    @Test
    fun droppingItOffEarnsABonus() {
        val dropOff = doc(coins = 300).copy(deliveryType = DELIVERY_DROP_OFF, dropOffNgoId = "ngo1", dropOffNgoName = "Hope Shelter")
        assertEquals(300 + DROP_OFF_BONUS, dropOff.totalCoins)
        assertEquals(300, doc(coins = 300).totalCoins)
        val item = dropOff.toDonationItem(now, ist)
        assertEquals(300 + DROP_OFF_BONUS, item.points)
        assertTrue(item.dropOff)
        assertEquals("Hope Shelter", item.ngo)
        val offer = dropOff.toNgoDonation(now, ist)
        assertTrue(offer.dropOff)
        assertEquals("ngo1", offer.dropOffNgoId)
    }

    @Test
    fun ngoSeesHowFarAwayItIs() {
        val there = doc().copy(place = Place(22.3110, 73.1700))
        assertEquals(2.42, there.toNgoDonation(now, ist, here = Place(22.3250, 73.1880)).distanceKm, 0.01)
        assertEquals(-1.0, there.toNgoDonation(now, ist).distanceKm, 0.0)
        assertEquals(-1.0, doc().toNgoDonation(now, ist, here = Place(22.3250, 73.1880)).distanceKm, 0.0)
        assertEquals(OfferStage.Accepted, doc(STATE_ACCEPTED).toNgoDonation(now, ist).stage)
        assertEquals(OfferStage.OnTheWay, doc(STATE_ON_THE_WAY).toNgoDonation(now, ist).stage)
    }

    @Test
    fun timestampsCanArriveAsDates() {
        val read = DonationDoc.fromMap(
            "x",
            mapOf("donorId" to "a", "title" to "Rice", "eatBy" to Date(now), "acceptedAt" to now - 5L, "lat" to 22.0, "lng" to 73L)
        )!!
        assertEquals(now, read.eatByMs)
        assertEquals(now - 5L, read.acceptedAtMs)
        assertEquals(Place(22.0, 73.0), read.place)
        assertEquals(DELIVERY_PICKUP, read.deliveryType)
        assertNull(DonationDoc.fromMap("y", mapOf("donorId" to "a", "title" to "Rice", "lat" to 22.0))!!.place)
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
    fun photoFlagTravelsToTheNgo() {
        val withPhoto = doc().copy(hasPhoto = true)
        assertEquals(true, withPhoto.toCreateMap()["hasPhoto"])
        assertTrue(DonationDoc.fromMap("d1", withPhoto.toCreateMap())!!.hasPhoto)
        assertTrue(withPhoto.toNgoDonation(now, ist).hasCloudPhoto)
        assertFalse(doc().toNgoDonation(now, ist).hasCloudPhoto)
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
