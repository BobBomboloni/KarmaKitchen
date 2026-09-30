sed -i 's/Log.e("GeminiService", "Failed to find nearby donors", e)/Log.w("GeminiService", "Failed to find nearby donors, falling back.")/g' ./app/src/main/java/com/example/api/GeminiService.kt
