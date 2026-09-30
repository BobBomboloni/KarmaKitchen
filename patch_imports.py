import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

imports = """
import androidx.compose.material.icons.filled.DirectionsCar
"""
if "import androidx.compose.material.icons.filled.DirectionsCar" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Favorite", "import androidx.compose.material.icons.filled.Favorite\n" + imports)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
