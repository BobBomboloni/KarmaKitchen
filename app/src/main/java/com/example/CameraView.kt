package com.example

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.AccentGold
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.PrimaryGreen
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onClose: () -> Unit,
    statusLabel: String = "Frame the food"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val preview = Preview.Builder().build()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(previewView) {
        val view = previewView ?: return@LaunchedEffect
        val cameraProvider = context.getCameraProvider()
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            cameraInstance = camera
            preview.setSurfaceProvider(view.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("CameraCapture", "Use case binding failed", exc)
        }
    }

    fun toggleTorch() {
        val newTorchState = !isTorchOn
        cameraInstance?.cameraControl?.enableTorch(newTorchState)
        isTorchOn = newTorchState
    }

    val accentGreen = PrimaryGreen

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview Feed
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Minimalist Smart Framing Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val boxWidth = w * 0.76f
            val boxHeight = h * 0.40f
            val left = (w - boxWidth) / 2f
            val top = (h - boxHeight) / 2.3f
            val cornerLen = 28.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // 4 Minimalist Corner Brackets
            // Top-Left
            val pathTL = Path().apply {
                moveTo(left, top + cornerLen)
                lineTo(left, top)
                lineTo(left + cornerLen, top)
            }
            drawPath(pathTL, accentGreen, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

            // Top-Right
            val pathTR = Path().apply {
                moveTo(left + boxWidth - cornerLen, top)
                lineTo(left + boxWidth, top)
                lineTo(left + boxWidth, top + cornerLen)
            }
            drawPath(pathTR, accentGreen, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

            // Bottom-Left
            val pathBL = Path().apply {
                moveTo(left, top + boxHeight - cornerLen)
                lineTo(left, top + boxHeight)
                lineTo(left + cornerLen, top + boxHeight)
            }
            drawPath(pathBL, accentGreen, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

            // Bottom-Right
            val pathBR = Path().apply {
                moveTo(left + boxWidth - cornerLen, top + boxHeight)
                lineTo(left + boxWidth, top + boxHeight)
                lineTo(left + boxWidth, top + boxHeight - cornerLen)
            }
            drawPath(pathBR, accentGreen, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }

        // Minimal Top Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Minimal Smart Framing Status
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }

            IconButton(
                onClick = { toggleTorch() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Flash",
                    tint = if (isTorchOn) AccentGold else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Minimal Shutter Button at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) accentGreen else Color.White)
                    .clickable(enabled = !isCapturing) {
                        isCapturing = true
                        val photoFile = File(
                            context.cacheDir,
                            "captured_food_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    onImageCaptured(Uri.fromFile(photoFile))
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    isCapturing = false
                                    onError(exc)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = OnPrimaryGreen,
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }
    }
}

suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(this))
    }
}
