package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoucherStoreTest {
    private fun voucher(id: String, code: String = "SWI-AAAA-BBBB") = Voucher(
        id = id, rewardId = "2", brand = "Swiggy", title = "₹500 gift card", code = code,
        boughtAt = 1_700_000_000_000L, paid = 950, validDays = 180
    )

    @Test
    fun jsonRoundTripKeepsEveryField() {
        val original = listOf(voucher("a"), voucher("b", code = ""))
        assertEquals(original, vouchersFromJson(vouchersToJson(original)))
    }

    @Test
    fun brokenJsonGivesAnEmptyList() {
        assertTrue(vouchersFromJson("not json").isEmpty())
    }

    @Test
    fun newPurchasesGoToTheTopOfTheList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        VoucherStore.vouchers.clear()
        VoucherStore.add(context, listOf(voucher("old")))
        VoucherStore.add(context, listOf(voucher("new1"), voucher("new2")))
        assertEquals(listOf("new1", "new2", "old"), VoucherStore.vouchers.map { it.id })
    }
}
