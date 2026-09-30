import sys

with open("./app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("fun FactItem(", "fun androidx.compose.foundation.layout.RowScope.FactItem(")

with open("./app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

