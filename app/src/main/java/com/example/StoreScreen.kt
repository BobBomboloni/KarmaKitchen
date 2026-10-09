package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cloud.Cloud
import com.example.cloud.CloudSync
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerColor
import com.example.ui.theme.MealsCardBg
import com.example.ui.theme.MealsTextPrimary
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.AmberText
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.SurfaceHighColor
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class StorePage { Browse, Cart, Receipt }
private enum class StoreTab { Shop, MyRewards }

private fun formatDay(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

// -----------------------------------------------------------------------------
// Screen
// -----------------------------------------------------------------------------

@Composable
fun KarmaStoreScreen(navController: NavController, userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    val context = LocalContext.current
    remember(context) { VoucherStore.load(context) }

    var page by remember { mutableStateOf(StorePage.Browse) }
    var tab by remember { mutableStateOf(StoreTab.Shop) }
    var category by remember { mutableStateOf<StoreCategory?>(null) }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var detail by remember { mutableStateOf<RewardItem?>(null) }
    var receipt by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    val listState = rememberLazyListState()

    val balance = userProfile.karmaPoints
    val tier = tierStatus(balance)
    val discount = tierDiscountPercent(tier.current)
    val total = cartTotal(cart, discount)

    BackHandler(enabled = page != StorePage.Browse) { page = StorePage.Browse }

    fun checkout() {
        if (cart.isEmpty() || total > balance) return
        val now = System.currentTimeMillis()
        val bought = storeRewards.flatMap { reward ->
            List(cart[reward.id] ?: 0) {
                Voucher(
                    id = UUID.randomUUID().toString(),
                    rewardId = reward.id,
                    brand = reward.brand,
                    title = reward.title,
                    code = if (reward.kind == RewardKind.Cause) "" else voucherCode(reward.brand),
                    boughtAt = now,
                    paid = discountedPrice(reward.points, discount),
                    validDays = reward.kind.validDays
                )
            }
        }
        if (Cloud.enabled) {
            // Saves the rewards and the coins spent together; the account's balance follows.
            CloudSync.purchase(bought, total)
        } else {
            VoucherStore.add(context, bought)
            onProfileUpdate(userProfile.copy(karmaPoints = balance - total))
        }
        receipt = bought
        cart = emptyMap()
        page = StorePage.Receipt
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        label = "storePage"
    ) { current ->
        when (current) {
            StorePage.Browse -> BrowsePage(
                listState = listState,
                balance = balance,
                tier = tier,
                discount = discount,
                tab = tab,
                onTab = { tab = it },
                category = category,
                onCategory = { category = it },
                cart = cart,
                total = total,
                onOpen = { detail = it },
                onAdd = { cart = addToCart(cart, it.id) },
                onRemove = { cart = removeFromCart(cart, it.id) },
                onOpenCart = { page = StorePage.Cart },
                onEarn = { navController.goToTab(Screen.Donate.route) },
                onBrowse = { tab = StoreTab.Shop }
            )
            StorePage.Cart -> CartPage(
                cart = cart,
                discount = discount,
                balance = balance,
                onBack = { page = StorePage.Browse },
                onAdd = { cart = addToCart(cart, it.id) },
                onRemove = { cart = removeFromCart(cart, it.id) },
                onCheckout = { checkout() }
            )
            StorePage.Receipt -> ReceiptPage(
                vouchers = receipt,
                email = userProfile.email,
                onMyRewards = {
                    tab = StoreTab.MyRewards
                    page = StorePage.Browse
                },
                onDone = {
                    tab = StoreTab.Shop
                    page = StorePage.Browse
                }
            )
        }
    }

    detail?.let { item ->
        RewardSheet(
            item = item,
            quantity = cart[item.id] ?: 0,
            discount = discount,
            short = coinsShort(total + discountedPrice(item.points, discount), balance),
            onClose = { detail = null },
            onAdd = {
                cart = addToCart(cart, item.id)
                detail = null
            },
            onEarn = {
                detail = null
                navController.goToTab(Screen.Donate.route)
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Browse
// -----------------------------------------------------------------------------

@Composable
private fun BrowsePage(
    listState: LazyListState,
    balance: Int,
    tier: TierStatus,
    discount: Int,
    tab: StoreTab,
    onTab: (StoreTab) -> Unit,
    category: StoreCategory?,
    onCategory: (StoreCategory?) -> Unit,
    cart: Map<String, Int>,
    total: Int,
    onOpen: (RewardItem) -> Unit,
    onAdd: (RewardItem) -> Unit,
    onRemove: (RewardItem) -> Unit,
    onOpenCart: () -> Unit,
    onEarn: () -> Unit,
    onBrowse: () -> Unit
) {
    val items = cartCount(cart)
    val myVouchers = VoucherStore.vouchers.toList()
    val visible = remember(category) { rewardsIn(category) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { StoreHeader(items, onOpenCart) }

            item {
                WalletCard(
                    balance = balance,
                    tier = tier,
                    discount = discount,
                    goal = nextGoal(balance, discount),
                    onEarn = onEarn,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { StoreTabs(tab, myVouchers.size, onTab) }

            if (tab == StoreTab.Shop) {
                item { CategoryChips(category, onCategory) }

                if (category == null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StoreSectionTitle("Popular right now")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(storeRewards.filter { it.featured }, key = { it.id }) { reward ->
                                    FeaturedCard(reward, discount) { onOpen(reward) }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StoreSectionTitle(category?.label ?: "All rewards")
                        if (category == StoreCategory.GiveBack) {
                            Text(
                                "Turn your coins into meals and trees. 100% goes to partner NGOs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            visible.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { reward ->
                                        val price = discountedPrice(reward.points, discount)
                                        RewardCard(
                                            item = reward,
                                            quantity = cart[reward.id] ?: 0,
                                            discount = discount,
                                            short = coinsShort(total + price, balance),
                                            onOpen = { onOpen(reward) },
                                            onAdd = { onAdd(reward) },
                                            onRemove = { onRemove(reward) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                if (myVouchers.isEmpty()) {
                    item { EmptyRewards(onBrowse) }
                } else {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            myVouchers.forEach { voucher -> VoucherCard(voucher) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = items > 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
        ) {
            CartBar(items, total, onOpenCart)
        }
    }
}

@Composable
private fun StoreHeader(cartItems: Int, onCart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Store", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Spend your coins on rewards", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        BadgedBox(
            badge = {
                if (cartItems > 0) {
                    Badge(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen) { Text("$cartItems") }
                }
            }
        ) {
            IconButton(onClick = onCart) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextPrimary, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun StoreSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun WalletCard(
    balance: Int,
    tier: TierStatus,
    discount: Int,
    goal: NextGoal?,
    onEarn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBalance = rememberCountedBalance(balance).value
    val progress by animateFloatAsState(tier.progress, tween(700), label = "tierProgress")
    val nextDiscount = tier.next?.let { tierDiscountPercent(it) } ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MealsCardBg)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your balance", style = MaterialTheme.typography.labelMedium, color = MealsTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatKarma(animatedBalance),
                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    KarmaCoin(size = 44.dp)
                }
            }
            Image(
                painter = painterResource(medalFor(tier.current)),
                contentDescription = "${tier.current} tier",
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = SecondaryAmber,
            trackColor = MealsTextPrimary.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                tier.next == null -> "${tier.current} tier: $discount% off everything in the store"
                nextDiscount > 0 -> "${formatKarma(tier.pointsToNext)} coins to ${tier.next}, then $nextDiscount% off everything"
                else -> "${formatKarma(tier.pointsToNext)} coins to ${tier.next}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary.copy(alpha = 0.85f)
        )
        if (discount > 0 && tier.next != null) {
            Text(
                "Your ${tier.current} perk: $discount% off everything",
                style = MaterialTheme.typography.labelMedium,
                color = MealsTextPrimary
            )
        }
        if (goal != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Next up: ${goal.reward.brand} ${goal.reward.title.lowercase()}, ${formatKarma(goal.coinsToGo)} coins to go",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(14.dp))
        KarmaOutlinedButton(
            onClick = onEarn,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MealsTextPrimary),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Text("Donate food to earn more", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StoreTabs(tab: StoreTab, myCount: Int, onTab: (StoreTab) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(4.dp)
    ) {
        StoreTab.entries.forEach { entry ->
            val selected = entry == tab
            val background by animateColorAsState(
                if (selected) PrimaryGreenLight else Color.Transparent,
                tween(200),
                label = "tabBackground"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(background)
                    .clickable { onTab(entry) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (entry == StoreTab.Shop) "Shop" else "My rewards",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) OnPrimaryGreenContainer else TextSecondary
                )
                if (entry == StoreTab.MyRewards && myCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$myCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PrimaryGreen else TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(selected: StoreCategory?, onSelect: (StoreCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { CategoryChip("All", selected == null) { onSelect(null) } }
        items(StoreCategory.entries.toList(), key = { it.name }) { category ->
            CategoryChip(category.label, selected == category) { onSelect(category) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) PrimaryGreenLight else SurfaceColor, tween(200), label = "chipBg")
    val border by animateColorAsState(if (selected) PrimaryGreen else OutlineColor, tween(200), label = "chipBorder")
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) OnPrimaryGreenContainer else TextSecondary,
        modifier = Modifier
            .bounceCard(CircleShape, onClick, background)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// -----------------------------------------------------------------------------
// Reward cards
// -----------------------------------------------------------------------------

@Composable
private fun FeaturedCard(item: RewardItem, discount: Int, onClick: () -> Unit) {
    val price = discountedPrice(item.points, discount)
    Box(
        modifier = Modifier
            .size(width = 264.dp, height = 148.dp)
            .bounceCard(RoundedCornerShape(20.dp), onClick, item.tileColor)
    ) {
        BrandLogoTile(
            item,
            72.dp,
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .rotate(6f)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(item.brand, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(item.title, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KarmaAmount(
                    formatKarma(price),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                if (discount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatKarma(item.points),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardCard(
    item: RewardItem,
    quantity: Int,
    discount: Int,
    short: Int,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val price = discountedPrice(item.points, discount)
    Column(
        modifier = modifier
            .bounceCard(RoundedCornerShape(16.dp), onOpen, SurfaceColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(item.tileColor),
            contentAlignment = Alignment.Center
        ) {
            BrandLogoTile(item, 56.dp)
            if (discount > 0) {
                Pill(
                    "$discount% off",
                    background = SecondaryAmber,
                    content = Color(0xFF3B2300),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
            if (quantity > 0) {
                Pill(
                    "$quantity in cart",
                    background = BackgroundColor.copy(alpha = 0.75f),
                    content = TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(item.brand, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceTag(price, if (discount > 0) item.points else null, Modifier.weight(1f))
                when {
                    quantity > 0 -> QuantityStepper(quantity, canAdd = short == 0, onAdd = onAdd, onRemove = onRemove)
                    short == 0 -> FilledTonalIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = PrimaryGreenLight,
                            contentColor = OnPrimaryGreenContainer
                        )
                    ) { Icon(Icons.Filled.Add, contentDescription = "Add to cart", modifier = Modifier.size(18.dp)) }
                    else -> Icon(Icons.Filled.Lock, contentDescription = "Not enough coins", tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
            if (short > 0 && quantity == 0) {
                Spacer(Modifier.height(4.dp))
                Text("Need ${formatKarma(short)} more", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun PriceTag(price: Int, original: Int?, modifier: Modifier = Modifier) {
    Column(modifier) {
        KarmaAmount(
            formatKarma(price),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (original != null) {
            Text(
                formatKarma(original),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textDecoration = TextDecoration.LineThrough
            )
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, canAdd: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceVariantColor)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Remove one", tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = TextPrimary,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onAdd, enabled = canAdd, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add one",
                tint = if (canAdd) TextPrimary else TextTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CartBar(count: Int, total: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .bounceCard(RoundedCornerShape(18.dp), onOpen, PrimaryGreen)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (count == 1) "1 item" else "$count items",
                style = MaterialTheme.typography.labelMedium,
                color = OnPrimaryGreen.copy(alpha = 0.8f)
            )
            KarmaAmount(
                formatKarma(total),
                style = MaterialTheme.typography.titleMedium,
                color = OnPrimaryGreen,
                fontWeight = FontWeight.Bold
            )
        }
        Text("View cart", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OnPrimaryGreen)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnPrimaryGreen)
    }
}

// -----------------------------------------------------------------------------
// Detail sheet
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RewardSheet(
    item: RewardItem,
    quantity: Int,
    discount: Int,
    short: Int,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onEarn: () -> Unit
) {
    val price = discountedPrice(item.points, discount)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val steps = if (item.kind == RewardKind.Cause) {
        listOf("Pay with your coins", "A partner NGO uses them within the week", "You get a thank-you note in My rewards")
    } else {
        listOf("Pay with your coins", "Your code appears in My rewards straight away", "Use it at checkout on the brand's app or website")
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = SurfaceHighColor,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(item.tileColor),
                contentAlignment = Alignment.Center
            ) {
                BrandLogoTile(item, 72.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "${item.brand} · ${item.kind.label}",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Text(item.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KarmaAmount(
                    formatKarma(price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (discount > 0) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        formatKarma(item.points),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Spacer(Modifier.width(10.dp))
                    Pill("$discount% off", SecondaryAmber, Color(0xFF3B2300))
                }
            }
            if (item.blurb.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(item.blurb, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(20.dp))
            Text("How it works", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            steps.forEach { SheetBullet(it) }

            Spacer(Modifier.height(16.dp))
            Text("Good to know", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            item.kind.terms.forEach { SheetBullet(it) }

            Spacer(Modifier.height(24.dp))
            if (short == 0) {
                KarmaButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen),
                    enabled = quantity < MAX_PER_REWARD
                ) {
                    Text(
                        if (quantity < MAX_PER_REWARD) "Add to cart" else "You have the most you can buy",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    "You need ${formatKarma(short)} more coins for this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                KarmaButton(
                    onClick = onEarn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                ) {
                    Text("Donate food to earn coins", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SheetBullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

// -----------------------------------------------------------------------------
// Cart
// -----------------------------------------------------------------------------

@Composable
private fun CartPage(
    cart: Map<String, Int>,
    discount: Int,
    balance: Int,
    onBack: () -> Unit,
    onAdd: (RewardItem) -> Unit,
    onRemove: (RewardItem) -> Unit,
    onCheckout: () -> Unit
) {
    val lines = storeRewards.filter { (cart[it.id] ?: 0) > 0 }
    val subtotal = cartSubtotal(cart)
    val total = cartTotal(cart, discount)
    val saved = subtotal - total
    val left = balance - total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Your cart", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = TextPrimary)
        }

        if (lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(R.drawable.illus_step_karma), contentDescription = null, modifier = Modifier.size(120.dp))
                Spacer(Modifier.height(12.dp))
                Text("Your cart is empty", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Add a reward and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                KarmaOutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("Browse rewards") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lines, key = { it.id }) { reward ->
                    val quantity = cart[reward.id] ?: 0
                    val price = discountedPrice(reward.points, discount)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceColor)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogoTile(reward, 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(reward.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2)
                            Text(reward.brand, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            PriceTag(price * quantity, if (discount > 0) reward.points * quantity else null)
                        }
                        QuantityStepper(
                            quantity = quantity,
                            canAdd = quantity < MAX_PER_REWARD && total + price <= balance,
                            onAdd = { onAdd(reward) },
                            onRemove = { onRemove(reward) }
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceColor)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryRow("Subtotal") { KarmaAmount(formatKarma(subtotal), style = MaterialTheme.typography.bodyMedium, color = TextPrimary) }
                        if (saved > 0) {
                            SummaryRow("Tier discount ($discount%)") {
                                KarmaAmount("-" + formatKarma(saved), style = MaterialTheme.typography.bodyMedium, color = AmberText)
                            }
                        }
                        HorizontalDivider(color = OutlineColor)
                        SummaryRow("Total", bold = true) {
                            KarmaAmount(formatKarma(total), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        SummaryRow("Balance after") {
                            KarmaAmount(
                                formatKarma(left),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (left >= 0) TextSecondary else DangerColor
                            )
                        }
                    }
                }
            }

            Column {
                HorizontalDivider(color = OutlineColor)
                KarmaButton(
                    onClick = onCheckout,
                    enabled = left >= 0,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                ) {
                    if (left >= 0) {
                        Text("Redeem for ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        KarmaAmount(
                            formatKarma(total),
                            style = MaterialTheme.typography.labelLarge,
                            color = OnPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("Not enough coins", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, bold: Boolean = false, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) TextPrimary else TextSecondary,
            modifier = Modifier.weight(1f)
        )
        value()
    }
}

// -----------------------------------------------------------------------------
// Receipt and my rewards
// -----------------------------------------------------------------------------

@Composable
private fun ReceiptPage(vouchers: List<Voucher>, email: String, onMyRewards: () -> Unit, onDone: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(pop.value)
                        .clip(CircleShape)
                        .background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Purchase complete", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (email.isBlank()) "Your rewards are saved in My rewards."
                    else "Your rewards are saved in My rewards. We also sent them to $email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        items(vouchers, key = { it.id }) { voucher -> VoucherCard(voucher) }
        item {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KarmaButton(
                    onClick = onMyRewards,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                ) { Text("See my rewards", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
                KarmaOutlinedButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Back to store", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun EmptyRewards(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.illus_step_karma), contentDescription = null, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(12.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            "Rewards you buy show up here with their codes.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        KarmaOutlinedButton(onClick = onBrowse, shape = RoundedCornerShape(12.dp)) { Text("Browse rewards") }
    }
}

@Composable
private fun VoucherCard(voucher: Voucher) {
    val reward = storeRewards.firstOrNull { it.id == voucher.rewardId }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    val until = validUntil(voucher.boughtAt, voucher.validDays)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (reward != null) {
                BrandLogoTile(reward, 48.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${voucher.brand} ${voucher.title}".trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (until != null) "Valid until ${formatDay(until)}" else "Given on ${formatDay(voucher.boughtAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            KarmaAmount(formatKarma(voucher.paid), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        if (voucher.code.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantColor)
                    .clickable {
                        clipboard.setText(AnnotatedString(voucher.code))
                        copied = true
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    voucher.code,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp, fontFeatureSettings = "tnum"),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Copied" else "Copy", style = MaterialTheme.typography.labelMedium, color = PrimaryGreen)
            }
        } else {
            Text(
                "Thank you. Your coins are on their way to partner NGOs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
