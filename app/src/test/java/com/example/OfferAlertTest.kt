package com.example

import com.example.cloud.STATE_OFFERED
import com.example.cloud.offerAlertText
import com.example.cloud.offerAlertTitle
import com.example.cloud.shouldAlert
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class OfferAlertTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private val me = "ngo-me"

    // 9 Oct 2026, 18:00 IST
    private val now = 1_791_549_000_000L

    // 9 Oct 2026, 21:30 IST
    private val ninePm = now + 3 * 3_600_000L + 30 * 60_000L

    private fun offer(vararg extra: Pair<String, Any?>): Map<String, Any?> =
        mapOf<String, Any?>("status" to STATE_OFFERED, "title" to "Veg biryani", "servings" to 12L) + extra

    @Test
    fun openOfferAlerts() {
        assertTrue(shouldAlert(offer(), me, now))
        assertTrue(shouldAlert(offer("dropOffNgoId" to null, "eatBy" to ninePm), me, now))
    }

    @Test
    fun dropOffForAnotherNgoDoesNotAlert() {
        assertFalse(shouldAlert(offer("deliveryType" to "donor_dropoff", "dropOffNgoId" to "ngo-other"), me, now))
    }

    @Test
    fun dropOffForMeAlerts() {
        assertTrue(shouldAlert(offer("deliveryType" to "donor_dropoff", "dropOffNgoId" to me), me, now))
    }

    @Test
    fun pastEatByDoesNotAlert() {
        assertFalse(shouldAlert(offer("eatBy" to now - 60_000L), me, now))
        assertFalse(shouldAlert(offer("eatBy" to now), me, now))
        assertFalse(shouldAlert(offer("eatBy" to Timestamp((now - 60_000L) / 1000, 0)), me, now))
    }

    @Test
    fun futureEatByAsTimestampAlerts() {
        assertTrue(shouldAlert(offer("eatBy" to Timestamp(ninePm / 1000, 0)), me, now))
    }

    @Test
    fun notOfferedDoesNotAlert() {
        assertFalse(shouldAlert(offer("status" to "accepted"), me, now))
        assertFalse(shouldAlert(offer("status" to null), me, now))
    }

    @Test
    fun notificationText() {
        assertEquals("New food offer: Veg biryani", offerAlertTitle(offer()))
        assertEquals("New food offer", offerAlertTitle(offer("title" to "")))
        assertEquals("12 servings · eat by 9:30 PM", offerAlertText(offer("eatBy" to Timestamp(ninePm / 1000, 0)), me, now, ist))
        assertEquals("1 serving", offerAlertText(offer("servings" to 1L), me, now, ist))
        assertEquals(
            "Eat by 10 Oct, 9:30 AM · the donor brings it to you",
            offerAlertText(
                mapOf("status" to STATE_OFFERED, "eatBy" to ninePm + 12 * 3_600_000L, "dropOffNgoId" to me),
                me,
                now,
                ist
            )
        )
    }
}
