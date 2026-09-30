import sys

with open("app/src/main/java/com/example/api/GeminiService.kt", "r") as f:
    content = f.read()

content = content.replace('\\"\\"\\"', '\"\"\"')

with open("app/src/main/java/com/example/api/GeminiService.kt", "w") as f:
    f.write(content)

