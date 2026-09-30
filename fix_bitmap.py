import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

new_lines = []
bitmap_count = 0
for line in lines:
    if "import android.graphics.Bitmap\n" == line or "import android.graphics.Bitmap" in line:
        if bitmap_count == 0:
            new_lines.append(line)
            bitmap_count += 1
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.writelines(new_lines)

