import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

imports = """
import com.example.api.IntakeAnalysisResult
import com.example.api.verifyIntakeWithGemini
import com.example.api.bitmapToBase64
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import android.graphics.Bitmap
"""

# add it after import com.example.api.FoodAnalysisResult
content = content.replace("import com.example.api.FoodAnalysisResult", "import com.example.api.FoodAnalysisResult\n" + imports)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

