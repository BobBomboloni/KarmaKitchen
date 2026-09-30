sed -i 's/object Tiers : Screen("tiers", "Impact Tiers", Icons.Filled.Star)/object Tiers : Screen("tiers", "Impact Tiers", Icons.Filled.Star)\n    object Map : Screen("map", "Food Map", Icons.Filled.LocationOn)/g' ./app/src/main/java/com/example/MainActivity.kt

sed -i 's/TierListScreen(navController = navController, userProfile = userProfile)/TierListScreen(navController = navController, userProfile = userProfile)\n            }\n            composable(Screen.Map.route) {\n                FoodMapScreen(navController = navController)/g' ./app/src/main/java/com/example/MainActivity.kt
