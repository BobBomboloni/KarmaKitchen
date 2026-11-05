package com.example

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure as AndroidPathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import com.example.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** How long the startup animation runs, in seconds. */
const val INTRO_SECONDS = 5f

/** From this moment the full wordmark stands still until the end. */
private const val REST_SECONDS = 4.75f

/**
 * The startup animation, "Seed of karma":
 *  1. On maroon, one grain of food falls and lands with a soft ring.
 *  2. The half-Hindi K grows up out of it like a plant (stem, arm, headline, the round bowl of the
 *     क, the leg) and its leaf unfurls: one small good deed grows into something bigger.
 *  3. A wave of the app's background spreads from the leaf, the K turns maroon and glides into
 *     place, the headline runs across, "arma" drops from it and "Kitchen" rises.
 *
 * It is drawn with Compose (no video), so it is sharp on every screen and follows dark mode. A tap
 * skips it. [onResting] is called once the wordmark has formed and stands still (the app can get
 * itself ready to show then, without a visible hitch), [onFinished] at the end. [frozenAt] draws one moment (in seconds)
 * without running, for previews and tests.
 */
@Composable
fun StartupAnimation(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onResting: () -> Unit = {},
    frozenAt: Float? = null,
) {
    val finish by rememberUpdatedState(onFinished)
    val rest by rememberUpdatedState(onResting)
    val clock = remember { Animatable(0f) }
    val pxPerUnit = growArtScale(LocalConfiguration.current, LocalDensity.current.density)
    // Previews and tests draw single frames, so they get the pictures straight away.
    var art by remember { mutableStateOf(if (frozenAt != null) GrowArt.makeOrNull(pxPerUnit) else null) }
    LaunchedEffect(frozenAt) {
        if (frozenAt == null) {
            // Get every logo shape and picture ready before the clock starts (a moment of plain
            // maroon), so no frame of the animation has to wait for them.
            if (art == null) {
                art = withContext(Dispatchers.Default) {
                    Intro.warmUp()
                    GrowArt.makeOrNull(pxPerUnit)
                }
            }
            // Right after launch the phone is still busy starting the app, which made the seed's
            // fall stutter now and then. Hold the plain maroon until frames come steadily.
            awaitSteadyFrames()
            clock.animateTo(REST_SECONDS, tween((REST_SECONDS * 1000).toInt(), easing = LinearEasing))
            rest()
            clock.animateTo(INTRO_SECONDS, tween(((INTRO_SECONDS - REST_SECONDS) * 1000).toInt(), easing = LinearEasing))
            finish()
        }
    }
    LightSystemBarsWhile { (frozenAt ?: clock.value) < 3.3f }

    val endBackground = MaterialTheme.colorScheme.background
    val palette = LocalPalette.current
    val karma = palette.wordmarkKarma
    val kitchen = palette.wordmarkKitchen
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = "KarmaKitchen" }
            .pointerInput(Unit) { detectTapGestures { finish() } }
    ) {
        drawIntro(frozenAt ?: clock.value, art, endBackground, karma, kitchen)
    }
}

/**
 * Waits until [steadyFrames] frames in a row arrive less than [lateMillis] apart (no big pauses
 * from the phone still starting the app), or at most [maxMillis].
 */
private suspend fun awaitSteadyFrames(steadyFrames: Int = 8, lateMillis: Long = 34, maxMillis: Long = 700) {
    val start = withFrameNanos { it }
    var last = start
    var steady = 0
    while (steady < steadyFrames && last - start < maxMillis * 1_000_000L) {
        val now = withFrameNanos { it }
        steady = if (now - last < lateMillis * 1_000_000L) steady + 1 else 0
        last = now
    }
}

/** White status and navigation bar icons while [condition] holds (the maroon part), then back. */
@Composable
private fun LightSystemBarsWhile(condition: () -> Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context.findActivityOrNull())?.window ?: return
    val controller = remember(window) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNav = controller.isAppearanceLightNavigationBars
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNav
        }
    }
    LaunchedEffect(controller) {
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNav = controller.isAppearanceLightNavigationBars
        snapshotFlow(condition).distinctUntilChanged().collect { onMaroon ->
            controller.isAppearanceLightStatusBars = if (onMaroon) false else lightStatus
            controller.isAppearanceLightNavigationBars = if (onMaroon) false else lightNav
        }
    }
}

private tailrec fun android.content.Context.findActivityOrNull(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext?.findActivityOrNull()
    else -> null
}

// ---------------------------------------------------------------------------------------------
// Timing helpers

private fun phase(t: Float, a: Float, b: Float): Float =
    if (b <= a) (if (t >= a) 1f else 0f) else ((t - a) / (b - a)).coerceIn(0f, 1f)

private fun easeOut(x: Float) = 1f - (1f - x).pow(3)
private fun easeInOut(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
private fun easeSine(x: Float) = (0.5f - 0.5f * cos(PI * x)).toFloat()

// ---------------------------------------------------------------------------------------------
// Drawing

private object Intro {
    // The pen strokes' paths, measured once. Only used on the main thread while drawing.
    val growMeasures: List<AndroidPathMeasure> by lazy {
        BrandArt.GROW.map { AndroidPathMeasure(BrandPaths.polyline(it.points).asAndroidPath(), false) }
    }
    // Reused for every stroke on every frame, so drawing makes no garbage.
    val growSegment = AndroidPath()
    // The part of the K each stroke may uncover (see BrandArt.GROW).
    val growRegions: List<Path> by lazy {
        BrandArt.GROW.map { stroke -> Path().apply { stroke.region.forEach { addPath(BrandPaths.polygon(it)) } } }
    }
    val kLocal: Path by lazy {
        Path().apply { BrandArt.K_LOCAL.forEach { r -> addRect(Rect(r[0], r[1], r[2], r[3])) } }
    }
    // A layer around the icon K (icon coordinates), big enough for the brush strokes.
    val kBounds = Rect(1150f, 200f, 1650f, 650f)

    /** Parses every logo path now, so that no frame of the animation has to stop for it. */
    fun warmUp() {
        BrandPaths.iconK; BrandPaths.bodyRegion; BrandPaths.leafRegion
        BrandPaths.wordmarkK; BrandPaths.arma; BrandPaths.kitchen
        growMeasures; growRegions; kLocal
    }
}

/**
 * How many pixels per icon unit the [GrowArt] pictures need: as sharp as the K is drawn while it
 * grows (the camera is zoomed in twice then), with a cap so the pictures stay a few MB at most.
 */
@Composable
private fun growArtScale(config: Configuration, density: Float): Float = remember(config, density) {
    val shortSide = min(config.screenWidthDp, config.screenHeightDp) * density
    (2f * shortSide * 0.46f / 244f).coerceIn(1f, 4.5f)
}

/**
 * Each part of the K painted once, before the animation starts, into its own picture. The growing
 * pen strokes are drawn with these pictures as their paint, so each stroke shows exactly its own
 * part of the K. This replaces offscreen layers and cut-outs that had to be worked out again on
 * every frame, which made the animation stutter on phones.
 */
private class GrowArt private constructor(val paints: List<AndroidPaint>) {
    companion object {
        fun makeOrNull(pxPerUnit: Float): GrowArt? =
            runCatching { make(pxPerUnit) }.getOrNull()

        private fun make(pxPerUnit: Float): GrowArt {
            val body = BrandPaths.bodyRegion.asAndroidPath()
            val k = BrandPaths.iconK.asAndroidPath()
            val fill = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = LogoCream.toArgb() }
            val paints = BrandArt.GROW.mapIndexed { i, stroke ->
                // The picture covers the stroke's part, within the K, plus a clear 2-unit edge.
                var left = Float.MAX_VALUE; var top = Float.MAX_VALUE
                var right = -Float.MAX_VALUE; var bottom = -Float.MAX_VALUE
                stroke.region.forEach { xy ->
                    for (j in xy.indices step 2) {
                        left = min(left, xy[j]); right = max(right, xy[j])
                        top = min(top, xy[j + 1]); bottom = max(bottom, xy[j + 1])
                    }
                }
                left = max(left, BrandArt.ICON_LEFT) - 2f
                top = max(top, BrandArt.ICON_TOP) - 2f
                right = min(right, BrandArt.ICON_RIGHT) + 2f
                bottom = min(bottom, BrandArt.ICON_BOTTOM) + 2f
                val bitmap = Bitmap.createBitmap(
                    ceil((right - left) * pxPerUnit).toInt(),
                    ceil((bottom - top) * pxPerUnit).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
                AndroidCanvas(bitmap).apply {
                    scale(pxPerUnit, pxPerUnit)
                    translate(-left, -top)
                    clipPath(body)
                    clipPath(Intro.growRegions[i].asAndroidPath())
                    drawPath(k, fill)
                }
                bitmap.prepareToDraw() // start sending it to the graphics chip now
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = stroke.width
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                        setLocalMatrix(AndroidMatrix().apply {
                            setScale(1f / pxPerUnit, 1f / pxPerUnit)
                            postTranslate(left, top)
                        })
                    }
                }
            }
            return GrowArt(paints)
        }
    }
}

private fun DrawScope.drawIntro(t: Float, art: GrowArt?, endBackground: Color, karma: Color, kitchen: Color) {
    val w = size.width
    val h = size.height

    // Final layout: the wordmark centred a little above the middle.
    val wmLeft = BrandArt.WM_LEFT; val wmRight = BrandArt.WM_RIGHT
    val sw = min(w * 0.8f, h * 1.6f) / (wmRight - wmLeft)
    val wcx = (wmLeft + wmRight) / 2f
    val wcy = (BrandArt.WM_TOP + BrandArt.WM_BOTTOM) / 2f
    val scx = w / 2f
    val scy = h * 0.47f
    val ks = BrandArt.WORDMARK_TO_ICON_SCALE
    val t0x = BrandArt.WORDMARK_TO_ICON_SHIFT[0]
    val t0y = BrandArt.WORDMARK_TO_ICON_SHIFT[1]
    val pivX = BrandArt.ICON_PIVOT[0]
    val pivY = BrandArt.ICON_PIVOT[1]
    val endX = ((pivX - t0x) / ks - wcx) * sw + scx
    val endY = ((pivY - t0y) / ks - wcy) * sw + scy
    val sEnd = sw / ks

    // Opening layout: the icon K large in the middle.
    val sA = min(w, h) * 0.46f / 244f
    val startX = w / 2f
    val startY = h * 0.47f

    // Camera: starts close on the grain and pulls back as the K grows.
    val grainX = BrandArt.GRAIN[0]
    val grainY = BrandArt.GRAIN[1]
    val zoomBack = easeInOut(phase(t, 1.0f, 2.6f))
    val zoom = 2f + (1f - 2f) * zoomBack
    val focusX = (grainX + 8f) + (pivX - (grainX + 8f)) * zoomBack
    val focusY = (grainY - 40f) + (pivY - (grainY - 40f)) * zoomBack
    val breathe = 0.94f + 0.06f * easeOut(phase(t, 0.3f, 3.0f))
    val move = easeInOut(phase(t, 3.12f, 3.68f))
    val scale = sA * breathe * zoom * (sEnd / sA).pow(move)
    val kx = startX + (endX - startX) * move
    val ky = startY + (endY - startY) * move

    // The leaf unfurls with a little spring.
    val tl = t - 2.25f
    val leafScale = if (tl > 0f) 1f - exp(-7f * tl) * cos(10f * tl) else 0f
    val leafTurn = if (tl > 0f) -14f * exp(-6f * tl) * cos(9f * tl) else -14f

    // Wave from the leaf.
    val rippleX = startX + (BrandArt.LEAF_CENTRE[0] - pivX) * sA
    val rippleY = startY + (BrandArt.LEAF_CENTRE[1] - pivY) * sA
    val rippleMax = hypot(max(rippleX, w - rippleX), max(rippleY, h - rippleY)) + 20f
    val ripple = rippleMax * easeInOut(phase(t, 3.0f, 3.5f))

    val kTransform: DrawTransform.() -> Unit = {
        translate(kx, ky)
        scale(scale, scale, pivot = Offset.Zero)
        translate(-focusX, -focusY)
    }

    // Scene 1: maroon, the grain and the growing K in cream.
    if (ripple < rippleMax) {
        drawRect(LogoMaroon)
        withTransform(kTransform) {
            drawGrowingK(t, art)
            if (leafScale > 0.001f) {
                val base = Offset(BrandArt.LEAF_BASE[0], BrandArt.LEAF_BASE[1])
                withTransform({
                    rotate(leafTurn, pivot = base)
                    scale(leafScale, leafScale, pivot = base)
                }) {
                    clipPath(BrandPaths.leafRegion) { drawPath(BrandPaths.iconK, LogoCream) }
                }
            }
            drawGrain(t, grainX, grainY)
        }
    }

    // Scene 2: inside the wave, the app's background with the wordmark coming together.
    if (ripple > 0f) {
        val wave = Path().apply { addOval(Rect(Offset(rippleX, rippleY), ripple)) }
        clipPath(wave) {
            drawRect(endBackground)
            withTransform(kTransform) {
                val iconAlpha = 1f - phase(t, 3.36f, 3.46f)
                val wordAlpha = phase(t, 3.28f, 3.38f)
                if (iconAlpha > 0f) layer(Intro.kBounds, iconAlpha) { drawIconK(karma, leafTurn, leafScale) }
                if (wordAlpha > 0f) {
                    withTransform({
                        translate(t0x, t0y)
                        scale(ks, ks, pivot = Offset.Zero)
                    }) {
                        layer(Rect(60f, 230f, 640f, 560f), wordAlpha) {
                            clipPath(Intro.kLocal) { drawPath(BrandPaths.wordmarkK, karma) }
                        }
                    }
                }
            }
            withTransform({
                translate(scx, scy)
                scale(sw, sw, pivot = Offset.Zero)
                translate(-wcx, -wcy)
            }) {
                val run = BrandArt.HEADLINE_RUN
                val headline = easeInOut(phase(t, 3.6f, 4.0f))
                if (headline > 0f) {
                    clipRect(run[0], run[1], run[0] + run[2] * headline, run[1] + run[3]) {
                        drawPath(BrandPaths.wordmarkK, karma)
                    }
                }
                BrandPaths.arma.forEach { (cx, path) ->
                    val f = (cx - 273f) / 347f
                    val e = easeOut(phase(t, 3.58f + 0.36f * f, 3.58f + 0.36f * f + 0.42f))
                    if (e > 0f) translate(0f, -26f * (1f - e)) { drawPath(path, karma, alpha = e) }
                }
                BrandPaths.kitchen.forEachIndexed { i, (_, path) ->
                    val e = easeOut(phase(t, 3.9f + i * 0.055f, 3.9f + i * 0.055f + 0.48f))
                    if (e > 0f) translate(0f, 28f * (1f - e)) { drawPath(path, kitchen, alpha = e) }
                }
            }
        }
    }
}

/** Draws [block] into an offscreen layer that is then blended at [alpha]. */
private inline fun DrawScope.layer(bounds: Rect, alpha: Float, block: DrawScope.() -> Unit) {
    if (alpha >= 1f) {
        block()
        return
    }
    drawIntoCanvas { it.saveLayer(bounds, Paint().apply { this.alpha = alpha }) }
    block()
    drawIntoCanvas { it.restore() }
}

/**
 * The body of the icon K (always cream), revealed by pen strokes that grow out of the grain. Each
 * stroke paints with the picture of its own part of the K (see [GrowArt]), so a wide brush never
 * uncovers stray bits of the parts next to it.
 */
private fun DrawScope.drawGrowingK(t: Float, art: GrowArt?) {
    val settle = phase(t, 2.2f, 2.4f)
    if (settle >= 1f) {
        clipPath(BrandPaths.bodyRegion) { drawPath(BrandPaths.iconK, LogoCream) }
        return
    }
    val strokes = BrandArt.GROW
    if (strokes.none { t > it.start }) return
    if (art == null) {
        // The pictures could not be made: let the K fade in instead of growing.
        val fade = phase(t, strokes.first().start, 2.4f)
        clipPath(BrandPaths.bodyRegion) { drawPath(BrandPaths.iconK, LogoCream, alpha = fade) }
        return
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val segment = Intro.growSegment
        strokes.forEachIndexed { i, stroke ->
            val raw = phase(t, stroke.start, stroke.end)
            val p = if (stroke.sine) easeSine(raw) else easeOut(raw)
            if (p <= 0f) return@forEachIndexed
            val measure = Intro.growMeasures[i]
            segment.rewind()
            measure.getSegment(0f, measure.length * p, segment, true)
            native.drawPath(segment, art.paints[i])
        }
    }
    if (settle > 0f) clipPath(BrandPaths.bodyRegion) { drawPath(BrandPaths.iconK, LogoCream, alpha = settle) }
}

/** The grain of food: falls, lands with a squash and a soft ring, then becomes the K's foot. */
private fun DrawScope.drawGrain(t: Float, gx: Float, gy: Float) {
    val fallStart = 0.15f
    val land = 0.7f
    if (t < fallStart) return
    var y = gy
    var sy = 1f
    if (t < land) {
        val p = phase(t, fallStart, land)
        y = gy - 320f * (1f - p * p)
    } else {
        val u = t - land
        sy = 1f - 0.35f * exp(-9f * u) * cos(16f * u)
    }
    val sx = 2f - sy
    val opacity = 1f - phase(t, 1.0f, 1.3f)
    val turn = -32f + 22f * phase(t, fallStart, land)
    if (opacity > 0f) {
        withTransform({
            translate(gx, y + 9.5f * (1f - sy))
            rotate(turn, pivot = Offset.Zero)
            scale(sx, sy, pivot = Offset.Zero)
        }) {
            drawOval(LogoCream, topLeft = Offset(-7.5f, -13f), size = Size(15f, 26f), alpha = opacity)
        }
    }
    if (t >= land) {
        val r = easeOut(phase(t, land, land + 0.7f))
        if (r < 1f) {
            drawOval(
                LogoCream,
                topLeft = Offset(gx - 46f * r, gy + 6f - 9f * r),
                size = Size(92f * r, 18f * r),
                alpha = 0.7f * (1f - r),
                style = Stroke(width = 3f)
            )
        }
    }
}
