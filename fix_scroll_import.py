import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

imports = """import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
"""

content = content.replace("import androidx.compose.foundation.background", imports + "import androidx.compose.foundation.background")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
