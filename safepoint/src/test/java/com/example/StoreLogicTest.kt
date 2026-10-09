package com.example

import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreLogicTest {
    private fun reward(id: String, price: Int, kind: RewardKind = RewardKind.GiftCard) =
        RewardItem(id, "Brand $id", "Card $id", price, "logo_$id", Color.Black, Color.White, kind = kind)

    private val rewards = listOf(reward("a", 1000), reward("b", 2000), reward("c", 250, RewardKind.Cause))

    @Test fun tiersGiveTheDiscountsShownOnTheTiersScreen() {
        assertEquals(0, tierDiscountPercent("Bronze"))
        assertEquals(5, tierDiscountPercent("Silver"))
        assertEquals(10, tierDiscountPercent("Gold"))
        assertEquals(20, tierDiscountPercent("Platinum"))
    }

    @Test fun discountedPriceRoundsToTheNearestCoin() {
        assertEquals(950, discountedPrice(1000, 5))
        assertEquals(238, discountedPrice(250, 5))
        assertEquals(1000, discountedPrice(1000, 0))
        assertEquals(0, discountedPrice(1000, 100))
        assertEquals(1000, discountedPrice(1000, -20))
    }

    @Test fun cartAddsAndRemovesOneAtATimeWithALimit() {
        var cart = emptyMap<String, Int>()
        repeat(MAX_PER_REWARD + 3) { cart = addToCart(cart, "a") }
        assertEquals(MAX_PER_REWARD, cart["a"])
        cart = removeFromCart(cart, "a")
        assertEquals(MAX_PER_REWARD - 1, cart["a"])
        repeat(MAX_PER_REWARD) { cart = removeFromCart(cart, "a") }
        assertTrue(cart.isEmpty())
        assertEquals(emptyMap<String, Int>(), removeFromCart(emptyMap(), "zzz"))
    }

    @Test fun cartTotalsUseTheTierDiscount() {
        val cart = mapOf("a" to 2, "b" to 1)
        assertEquals(3, cartCount(cart))
        assertEquals(4000, cartSubtotal(cart, rewards))
        assertEquals(3800, cartTotal(cart, 5, rewards))
        assertEquals(4000, cartTotal(cart, 0, rewards))
    }

    @Test fun coinsShortIsNeverNegative() {
        assertEquals(240, coinsShort(1000, 760))
        assertEquals(0, coinsShort(1000, 5000))
    }

    @Test fun nextGoalIsTheNearestPartnerRewardOutOfReach() {
        val goal = nextGoal(balance = 1240, discountPercent = 0, rewards = rewards)
        assertNotNull(goal)
        assertEquals("b", goal!!.reward.id)
        assertEquals(760, goal.coinsToGo)
        // Give-back rewards are never suggested as the next goal.
        assertNull(nextGoal(balance = 5000, discountPercent = 0, rewards = rewards))
    }

    @Test fun voucherCodesHaveTheExpectedShape() {
        val code = voucherCode("Swiggy", Random(42))
        assertTrue(code, Regex("^SWI-[A-Z2-9]{4}-[A-Z2-9]{4}$").matches(code))
        assertTrue(Regex("^XXX-").containsMatchIn(voucherCode("", Random(1))))
        assertEquals(voucherCode("Nike", Random(7)), voucherCode("Nike", Random(7)))
    }

    @Test fun vouchersExpireAfterTheirValidity() {
        val bought = 1_000_000_000_000L
        assertEquals(bought + 180 * DAY_MILLIS, validUntil(bought, 180))
        assertNull(validUntil(bought, 0))
    }

    @Test fun categoriesFilterTheCatalogue() {
        assertEquals(storeRewards.size, rewardsIn(null).size)
        StoreCategory.entries.forEach { category ->
            val list = rewardsIn(category)
            assertTrue("Nothing in ${category.label}", list.isNotEmpty())
            assertTrue(list.all { it.category == category })
        }
    }

    @Test fun catalogueIsConsistent() {
        assertEquals(storeRewards.size, storeRewards.map { it.id }.toSet().size)
        assertTrue(storeRewards.all { it.points > 0 })
        assertTrue(storeRewards.filter { it.kind == RewardKind.Cause }.all { it.category == StoreCategory.GiveBack })
        // The home banner shows the first three partners.
        assertEquals(listOf("McDonald's", "Swiggy", "Spotify"), storeRewards.take(3).map { it.brand })
        assertTrue(homeRewards.none { it.kind == RewardKind.Cause })
    }
}
