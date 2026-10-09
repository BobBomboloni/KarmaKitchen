package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmileStoreTest {

    private fun entry(id: String, donationId: String = "sandwiches") = SmileEntry(
        id = id,
        donationId = donationId,
        donationTitle = "Leftover Catering Sandwiches",
        ngoName = "Navrachana Community",
        message = "Thank you!",
        people = 18,
        photoPath = "/data/smiles/$id.jpg",
        sentAt = 1_700_000_000_000L
    )

    @Test
    fun jsonRoundTripKeepsEveryField() {
        val original = listOf(entry("a"), entry("b", "bakery"), entry("c").copy(isExample = true))
        assertEquals(original, smilesFromJson(smilesToJson(original)))
    }

    @Test
    fun receiverListsItsOwnAndTheExampleSmilesAsSent() {
        assertTrue(entry("a").isSentByThisNgo())
        assertTrue(entry("b").copy(ngoName = "Hope Shelter", isExample = true).isSentByThisNgo())
        assertFalse(entry("c").copy(ngoName = "Hope Shelter").isSentByThisNgo())
    }

    @Test
    fun brokenJsonGivesAnEmptyList() {
        assertTrue(smilesFromJson("not json").isEmpty())
    }

    @Test
    fun cloudSmilesReplaceEverythingButTheExamples() {
        SmileStore.smiles.clear()
        SmileStore.smiles.add(entry("ex").copy(isExample = true, sentAt = 1_000L))
        SmileStore.smiles.add(entry("signed-out-user").copy(sentAt = 5_000L))

        SmileStore.replaceCloud(listOf(entry("new").copy(sentAt = 2_000L), entry("newer").copy(sentAt = 3_000L)))
        assertEquals(listOf("newer", "new", "ex"), SmileStore.smiles.map { it.id })

        SmileStore.replaceCloud(emptyList())
        assertEquals(listOf("ex"), SmileStore.smiles.map { it.id })
    }

    @Test
    fun addAndRemoveUpdateTheStore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SmileStore.smiles.clear()

        assertFalse(SmileStore.hasSmileFor("sandwiches"))
        SmileStore.add(context, entry("a"))
        assertTrue(SmileStore.hasSmileFor("sandwiches"))
        assertEquals("a", SmileStore.smiles.first().id)

        SmileStore.remove(context, "a")
        assertFalse(SmileStore.hasSmileFor("sandwiches"))
    }
}
