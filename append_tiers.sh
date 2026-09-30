cat << 'INNER_EOF' >> ./app/src/main/java/com/example/MainActivity.kt

@Composable
fun TierListScreen(navController: NavController, userProfile: UserProfile) {
    val tiers = listOf(
        TierInfo("Bronze", 0, "Default 1x KP multiplier", Color(0xFFCD7F32)),
        TierInfo("Silver", 2000, "1.2x KP multiplier\n5% store discount", Color(0xFFC0C0C0)),
        TierInfo("Gold", 5000, "1.5x KP multiplier\n10% store discount", Color(0xFFFFD700)),
        TierInfo("Platinum", 10000, "2.0x KP multiplier\n20% store discount\nFree shipping", Color(0xFFE5E4E2))
    )

    val currentTier = when {
        userProfile.karmaPoints >= 10000 -> tiers[3]
        userProfile.karmaPoints >= 5000 -> tiers[2]
        userProfile.karmaPoints >= 2000 -> tiers[1]
        else -> tiers[0]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Impact Tiers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = currentTier.color.copy(alpha = 0.2f)),
                    border = BorderStroke(2.dp, currentTier.color.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Current Tier",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentTier.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = currentTier.color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%,d KP".format(userProfile.karmaPoints),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Tiers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(tiers.size) { index ->
                val tier = tiers[index]
                val nextTierKp = if (index < tiers.size - 1) tiers[index + 1].minKp else null
                val rangeText = if (nextTierKp != null) {
                    "%,d - %,d KP".format(tier.minKp, nextTierKp - 1)
                } else {
                    "%,d+ KP".format(tier.minKp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (tier.name == currentTier.name) tier.color else OutlineColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(tier.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = tier.color,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tier.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tier.color
                                )
                                if (tier.name == currentTier.name) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tier.color)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                            Text(
                                text = tier.benefits,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TierInfo(
    val name: String,
    val minKp: Int,
    val benefits: String,
    val color: Color
)
INNER_EOF
