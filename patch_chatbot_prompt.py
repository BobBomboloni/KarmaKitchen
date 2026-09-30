import sys

filepath = "app/src/main/java/com/example/api/GeminiService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = 'val systemInstruction = "You are a helpful AI assistant for KarmaKitchen. You help users with queries related to food donation, food storage, safety, and disposal. Be concise, friendly, and practical. Do not use Markdown, just plain text if possible, or very simple formatting."'

replacement = 'val systemInstruction = "You are a helpful AI assistant for KarmaKitchen. Your MAIN priority is answering queries related to food donation. You also help with food storage and food safety. If a user asks about topics unrelated to food donation, storage, or safety, politely decline and steer the conversation back. Be concise, friendly, and practical. Do not use Markdown, just plain text if possible, or very simple formatting."'

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Patched GeminiService successfully")
else:
    print("Target not found")
