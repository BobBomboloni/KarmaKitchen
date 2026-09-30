sed -i 's/    object Map : Screen("map", "Food Map", Icons.Filled.LocationOn)//g' ./app/src/main/java/com/example/MainActivity.kt

sed -i '/composable(Screen.Map.route) {/,/}/d' ./app/src/main/java/com/example/MainActivity.kt

cat << 'INNER_EOF' > fix_who_needs.py
import sys
with open("./app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Who Needs Food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = { navController.navigate(Screen.Map.route) }, contentPadding = PaddingValues(0.dp)) {
                    Text("View Map", color = PrimaryGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }"""

replacement = """            Text("Who Needs Food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)"""

content = content.replace(target, replacement)
with open("./app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
INNER_EOF
python3 fix_who_needs.py
