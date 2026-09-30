import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """            var recomposeTrigger by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(200)
                    recomposeTrigger++
                }
            }
            
            val elapsedSeconds = (System.currentTimeMillis() - AppStartTime) / 1000.0"""

replacement = """            var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(200)
                    currentTime = System.currentTimeMillis()
                }
            }
            
            val elapsedSeconds = (currentTime - AppStartTime) / 1000.0"""

if target in content:
    content = content.replace(target, replacement)
else:
    print("Target not found!")
    
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

