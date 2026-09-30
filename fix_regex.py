import sys

with open("app/src/main/java/com/example/api/GeminiService.kt", "r") as f:
    content = f.read()

content = content.replace('.replace(Regex("^```json\s*",', '.replace(Regex("^```json\\\\s*",')
content = content.replace('.replace(Regex("^```\s*"), "")', '.replace(Regex("^```\\\\s*"), "")')

with open("app/src/main/java/com/example/api/GeminiService.kt", "w") as f:
    f.write(content)

