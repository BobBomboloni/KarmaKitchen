package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DonationLogTest {
    @Before fun startFresh() = DonationLog.clear()

    private fun delivered(id: String) = DonationItem(
        "Veg pulao", NGO_NAME, "Just now", STATUS_DELIVERED, 600, FoodCategory.Meals, stage = 2, id = id
    )

    @Test fun aNewDonationHasNotBeenAcknowledged() {
        assertFalse(delivered("a").acknowledged)
    }

    @Test fun acknowledgingOnlyChangesThatDonation() {
        DonationLog.add(delivered("a"))
        DonationLog.add(delivered("b"))
        DonationLog.acknowledge("a")
        assertTrue(DonationLog.submitted.first { it.id == "a" }.acknowledged)
        assertFalse(DonationLog.submitted.first { it.id == "b" }.acknowledged)
        assertEquals(2, DonationLog.submitted.size)
    }

    @Test fun deliveredDonationsAreNotInTransit() {
        assertFalse(delivered("a").inTransit)
    }
}
