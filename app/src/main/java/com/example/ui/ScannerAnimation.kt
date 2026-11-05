package com.example.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * "Hologram mesh": shown while the AI looks at a food photo. The photo becomes a still 3D wire mesh
 * on a dark panel, seen from an angle. Green rings ripple out from the middle like a radar, and as a
 * ring passes, the food rises into 3D bumps (bright, colourful parts rise most, the table stays low).
 * Then the mesh settles flat and it starts again.
 *
 * Kept light on purpose: the photo is read once, at 40 x 36 points, on a background thread. Each
 * frame does a little maths per point and draws the mesh as a handful of paths (lines are sorted
 * into a few colour buckets), reading the clock in the draw phase so nothing else is rebuilt.
 */

private const val NX = 40
private const val NY = 36
private const val CYCLE_MS = 5600

private val PanelDark = Color(0xFF170D09)
private val PanelGlow = Color(0x737A0F1C)        // logo maroon, soft
private val PillText = Color(0xFFFFF8EE)
private val PillFill = Color(0x8C1C110C)
private val PillEdge = Color(0x59C9DB7E)

/** Mesh line colours: low ground (maroon-pink), raised food (warm cream), where the ring is (green). */
private val MeshHues = arrayOf(Color(0xFFBE5A64), Color(0xFFECBEAA), Color(0xFFD6E88C))
private const val LEVELS = 8
private const val BUCKETS = 3 * LEVELS
private const val PITCH = 0.85f

/** The photo, boiled down to one height per point: bright, colourful parts (food) are high. */
class ScanMap internal constructor(internal val height: FloatArray)

/** A flat field used until the photo has been read (or when there is no photo). */
private val PlainMap: ScanMap by lazy { ScanMap(FloatArray(NX * NY)) }

private fun scanMapOf(bitmap: Bitmap): ScanMap {
    val small = Bitmap.createScaledBitmap(bitmap, NX, NY, true)
    val px = IntArray(NX * NY)
    small.getPixels(px, 0, NX, 0, 0, NX, NY)
    if (small !== bitmap) small.recycle()
    return ScanMap(FloatArray(px.size) { i ->
        val c = px[i]
        val r = ((c shr 16) and 0xFF) / 255f
        val g = ((c shr 8) and 0xFF) / 255f
        val b = (c and 0xFF) / 255f
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val lum = r * 0.3f + g * 0.59f + b * 0.11f
        val sat = if (hi > 0f) (hi - lo) / hi else 0f
        min(1f, 1.3f * sat * lum + 0.35f * lum * lum)
    })
}

/** Reads a photo from [uri] into a scan map, off the main thread. Null until it is ready. */
@Composable
fun rememberScanMap(uri: Uri?): ScanMap? {
    val context = LocalContext.current
    val map by produceState<ScanMap?>(null, uri) {
        if (uri == null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(NX * 4, NY * 4)
                    .scale(Scale.FILL)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .build()
                val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
                (drawable as? BitmapDrawable)?.bitmap?.let(::scanMapOf)
            }.getOrNull()
        }
    }
    return map
}

/** The same, from a bitmap the app already has (the NGO's camera photo). */
@Composable
fun rememberScanMap(bitmap: Bitmap?): ScanMap? {
    val map by produceState<ScanMap?>(null, bitmap) {
        if (bitmap == null) return@produceState
        value = withContext(Dispatchers.Default) { runCatching { scanMapOf(bitmap) }.getOrNull() }
    }
    return map
}

/**
 * The full scanning panel: the hologram mesh with the current step in a pill at the bottom.
 * [message] fades when it changes.
 */
@Composable
fun FoodDepthScan(map: ScanMap?, message: String, modifier: Modifier = Modifier) {
    Box(modifier.semantics { contentDescription = "Scanning your food photo" }) {
        HoloMesh(map ?: PlainMap, Modifier.fillMaxSize())
        ScanStatusPill(
            message,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun HoloMesh(map: ScanMap, modifier: Modifier) {
    val clock = rememberInfiniteTransition(label = "mesh").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "cycle"
    )
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val u = w / 328f                                   // the design was made 328 wide
            val glow = Brush.radialGradient(
                listOf(PanelGlow, Color.Transparent),
                center = Offset(w / 2f, h * 0.45f),
                radius = 240f * u
            )
            val cell = w * 0.83f / NX
            val focal = 1.28f * w
            val lift = 0.244f * w
            val cx = w / 2f
            val cy = h * 0.47f
            val cp = cos(PITCH)
            val sp = sin(PITCH)
            // Where each point sits on the flat grid, and how far it is from the middle (for the rings).
            val n = NX * NY
            val gx = FloatArray(n) { (it % NX - NX / 2f + 0.5f) * cell }
            val gz = FloatArray(n) { (NY / 2f - 0.5f - it / NX) * cell }
            val dist = FloatArray(n) {
                val dx = (it % NX - NX / 2f + 0.5f) / NX
                val dy = (it / NX - NY / 2f + 0.5f) / NY * 1.1f
                sqrt(dx * dx + dy * dy)
            }
            // Per-frame results, reused.
            val sx = FloatArray(n)
            val sy = FloatArray(n)
            val lifted = FloatArray(n)
            val lit = FloatArray(n)
            val paths = Array(BUCKETS) { Path() }
            val stroke = Stroke(width = 1f * u, cap = StrokeCap.Round)

            fun bucketOf(a: Int, b: Int, boost: Float): Int {
                val band = (lit[a] + lit[b]) / 2f
                val hh = (lifted[a] + lifted[b]) / (2f * u)
                val alpha = min(1f, (0.16f + 0.5f * min(1f, hh / 35f)) * boost + band * 0.7f)
                val level = ((alpha * LEVELS).toInt()).coerceIn(0, LEVELS - 1)
                val hue = if (band > 0.15f) 2 else if (hh > 14f) 1 else 0
                return hue * LEVELS + level
            }

            onDrawBehind {
                drawRect(PanelDark)
                drawRect(glow)

                val sec = clock.value * CYCLE_MS / 1000f
                val settle = 1f - smooth(4.9f, 5.5f, sec)
                val r1 = sec / 4.4f * 1.25f
                val r2 = r1 - 0.33f
                for (k in 0 until n) {
                    val d1 = (dist[k] - r1) / 0.035f
                    val d2 = (dist[k] - r2) / 0.035f
                    val band = max(exp(-d1 * d1), 0.7f * exp(-d2 * d2)) * settle
                    val up = max(if (dist[k] < r1) 1f else 0f, band) * settle
                    val y = map.height[k] * lift * up + band * 7f * u
                    val z1 = gz[k]
                    val y2 = -y * cp - z1 * sp
                    val z2 = -y * sp + z1 * cp
                    val scale = focal / (focal + z2 + 60f * u)
                    sx[k] = cx + gx[k] * scale
                    sy[k] = cy + y2 * scale
                    lifted[k] = y
                    lit[k] = band
                }
                paths.forEach { it.reset() }
                for (j in 0 until NY) for (i in 0 until NX - 1) {
                    val a = j * NX + i
                    val b = a + 1
                    paths[bucketOf(a, b, 1f)].apply { moveTo(sx[a], sy[a]); lineTo(sx[b], sy[b]) }
                }
                for (i in 0 until NX) for (j in 0 until NY - 1) {
                    val a = j * NX + i
                    val b = a + NX
                    paths[bucketOf(a, b, 0.8f)].apply { moveTo(sx[a], sy[a]); lineTo(sx[b], sy[b]) }
                }
                for (bucket in 0 until BUCKETS) {
                    val hue = bucket / LEVELS
                    val alpha = (bucket % LEVELS + 0.5f) / LEVELS
                    drawPath(paths[bucket], MeshHues[hue].copy(alpha = alpha), style = stroke)
                }
            }
        }
    )
}

/** The rounded pill with the current step, glassy on the dark panel. */
@Composable
fun ScanStatusPill(message: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier
            .clip(shape)
            .background(PillFill)
            .border(1.dp, PillEdge, shape)
            .padding(horizontal = 15.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        AnimatedContent(
            targetState = message,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "scanStep"
        ) { text ->
            Text("$text…", style = MaterialTheme.typography.labelLarge, color = PillText)
        }
    }
}

private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
