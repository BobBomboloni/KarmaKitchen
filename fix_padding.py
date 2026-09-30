import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = "contentPadding = PaddingValues(horizontal = 24.dp, bottom = 24.dp),"
replacement = "contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),"

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

