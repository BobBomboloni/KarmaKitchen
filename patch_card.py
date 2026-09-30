import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF34C759))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(delivery.first, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${delivery.second} • ${delivery.third}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    // Actions inside card
                    Button(
                        onClick = { /* Log Intake */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) {
                        Text("Accept & Log AI Intake", color = Color.Black, fontWeight = FontWeight.Bold)
                    }"""

replacement = """                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF34C759))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(delivery.first, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${delivery.second} • ${delivery.third}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                        // Actions inside card
                        Button(
                            onClick = { /* Log Intake */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, topEnd = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                        ) {
                            Text("Accept & Log AI Intake", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
