import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Operational Command", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                    Text("Navrachana Community", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                }
            }
        }"""

replacement = """        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_karma_logo),
                        contentDescription = "KarmaKitchen Logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Operational Command", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                        Text("Navrachana Community", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                }
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
