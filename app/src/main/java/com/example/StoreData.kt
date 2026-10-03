package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BrandBlack
import com.example.ui.theme.BrandMcDonaldsGold
import com.example.ui.theme.BrandMcDonaldsRed
import com.example.ui.theme.BrandNetflixBlack
import com.example.ui.theme.BrandNetflixRed
import com.example.ui.theme.BrandSamsungBlue
import com.example.ui.theme.BrandSpotifyBlack
import com.example.ui.theme.BrandSpotifyGreen
import com.example.ui.theme.BrandStarbucksGreen
import com.example.ui.theme.BrandSwiggyOrange
import com.example.ui.theme.BrandWhite
import com.example.ui.theme.BrandZomatoRed

/** What kind of reward this is; decides how long it lasts and what the fine print says. */
enum class RewardKind(val label: String, val validDays: Int, val terms: List<String>) {
    GiftCard(
        "Gift card", 180,
        listOf("Valid for 6 months from the day you buy it", "Can't be exchanged for cash", "The code works once")
    ),
    Coupon(
        "Coupon", 90,
        listOf("Valid for 3 months from the day you buy it", "One use per order", "Can't be combined with other offers")
    ),
    Cause(
        "Give back", 0,
        listOf(
            "Your coins go to partner NGO kitchens and volunteers",
            "You get a thank-you note, so there is no code to use",
            "Coins given this way can't be refunded"
        )
    )
}

enum class StoreCategory(val label: String) {
    Food("Food"),
    Entertainment("Entertainment"),
    Shopping("Shopping"),
    GiveBack("Give back")
}

/**
 * Something the donor can buy with karma coins. [logoName] is the name of a drawable
 * (res/drawable/<logoName>) holding a partner's logo; if that file is missing the tile shows the
 * brand name instead. With [tintLogo] the logo is painted in [logoColor] on a [tileColor] tile.
 * Give-back rewards have no logo and use an illustration ([art]) instead.
 */
data class RewardItem(
    val id: String,
    val brand: String,
    val title: String,
    val points: Int,
    val logoName: String,
    val tileColor: Color,
    val logoColor: Color,
    val tintLogo: Boolean = true,
    val logoScale: Float = 0.62f,
    val category: StoreCategory = StoreCategory.Shopping,
    val kind: RewardKind = RewardKind.GiftCard,
    val blurb: String = "",
    val featured: Boolean = false,
    val art: Int? = null
)

private val GiveBackMealTile = Color(0xFF4A3414)
private val GiveBackFamilyTile = Color(0xFF17384A)
private val GiveBackTreeTile = Color(0xFF1F4A2A)

/**
 * The Karma Store catalogue. The first three entries are used by the home banner, so keep
 * McDonald's, Swiggy and Spotify at the top.
 */
val storeRewards = listOf(
    RewardItem(
        "1", "McDonald's", "₹500 gift card", 1000, "logo_mcdonalds", BrandMcDonaldsRed, BrandMcDonaldsGold,
        category = StoreCategory.Food, featured = true,
        blurb = "Burgers, fries and McCafé drinks at McDonald's restaurants across India."
    ),
    RewardItem(
        "2", "Swiggy", "₹500 gift card", 1000, "logo_swiggy", BrandSwiggyOrange, BrandWhite,
        category = StoreCategory.Food, featured = true,
        blurb = "Order meals or groceries to your door with Swiggy."
    ),
    RewardItem(
        "3", "Spotify", "₹500 gift card", 1000, "logo_spotify", BrandSpotifyBlack, BrandSpotifyGreen,
        category = StoreCategory.Entertainment, featured = true,
        blurb = "Add credit to your Spotify account for Premium listening."
    ),
    RewardItem(
        "4", "Samsung", "10% off coupon", 2000, "logo_samsung", BrandSamsungBlue, BrandWhite, logoScale = 0.78f,
        category = StoreCategory.Shopping, kind = RewardKind.Coupon,
        blurb = "10% off phones, tablets and accessories on Samsung.com."
    ),
    RewardItem(
        "5", "Puma", "₹1,000 gift card", 2500, "logo_puma", BrandWhite, BrandBlack,
        category = StoreCategory.Shopping,
        blurb = "Shoes and sportswear at Puma stores and puma.com."
    ),
    RewardItem(
        "6", "Nike", "₹2,500 gift card", 5000, "logo_nike", BrandWhite, BrandBlack,
        category = StoreCategory.Shopping,
        blurb = "Trainers, kit and apparel at Nike stores and nike.com."
    ),
    RewardItem(
        "7", "Zomato", "₹500 gift card", 1000, "logo_zomato", BrandZomatoRed, BrandWhite, logoScale = 0.8f,
        category = StoreCategory.Food,
        blurb = "Order in or book a table with Zomato."
    ),
    RewardItem(
        "8", "Starbucks", "₹300 gift card", 600, "logo_starbucks", BrandStarbucksGreen, BrandWhite, logoScale = 0.7f,
        category = StoreCategory.Food,
        blurb = "Coffee, tea and snacks at Starbucks cafés."
    ),
    RewardItem(
        "9", "Netflix", "₹500 gift card", 1000, "logo_netflix", BrandNetflixBlack, BrandNetflixRed, logoScale = 0.55f,
        category = StoreCategory.Entertainment,
        blurb = "Add credit to your Netflix account."
    ),
    RewardItem(
        "10", "Partner NGOs", "Sponsor 5 meals", 250, "", GiveBackMealTile, BrandWhite,
        category = StoreCategory.GiveBack, kind = RewardKind.Cause,
        blurb = "Your coins pay for 5 meals cooked and served by a partner NGO kitchen.",
        art = R.drawable.illus_food_meal
    ),
    RewardItem(
        "11", "Partner NGOs", "Feed a family for a week", 1000, "", GiveBackFamilyTile, BrandWhite,
        category = StoreCategory.GiveBack, kind = RewardKind.Cause,
        blurb = "A week of groceries for one family, delivered by an NGO partner.",
        art = R.drawable.illus_food_pack
    ),
    RewardItem(
        "12", "Partner NGOs", "Plant 3 trees", 400, "", GiveBackTreeTile, BrandWhite,
        category = StoreCategory.GiveBack, kind = RewardKind.Cause,
        blurb = "Three saplings planted by local volunteers near where food is grown.",
        art = R.drawable.illus_cause_tree
    )
)

/** Rewards for the home screen: partner brands only. */
val homeRewards: List<RewardItem> get() = storeRewards.filter { it.kind != RewardKind.Cause }

/** Rewards in [category] (all of them when it is null). */
fun rewardsIn(category: StoreCategory?, rewards: List<RewardItem> = storeRewards): List<RewardItem> =
    rewards.filter { category == null || it.category == category }
