package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPalette

/** The logo colours as they are in the artwork (the app icon never changes with dark mode). */
internal val LogoMaroon = Color(0xFF7A0F1C)
internal val LogoGreen = Color(0xFF5C7616)
internal val LogoCream = Color(0xFFFCF7F2)

/** The logo paths, parsed once (they are large). */
internal object BrandPaths {
    val wordmarkK: Path by lazy { parse(BrandArt.WORDMARK_K) }
    val arma: List<Pair<Float, Path>> by lazy { BrandArt.ARMA.map { (cx, d) -> cx to parse(d) } }
    val kitchen: List<Pair<Float, Path>> by lazy { BrandArt.KITCHEN.map { (cx, d) -> cx to parse(d) } }
    val iconK: Path by lazy { parse(BrandArt.ICON_K) }
    val leafRegion: Path by lazy { polygon(BrandArt.LEAF_REGION) }
    val bodyRegion: Path by lazy { polygon(BrandArt.BODY_REGION) }
    /** The wordmark's leaf: everything of the K above the headline, right of the K's top. */
    val wordmarkLeafRegion: Path by lazy { polygon(floatArrayOf(236f, 250f, 360f, 250f, 360f, 338.5f, 236f, 338.5f)) }

    fun parse(d: String): Path = PathParser().parsePathString(d).toPath().apply { fillType = PathFillType.EvenOdd }

    fun polygon(xy: FloatArray): Path = Path().apply {
        moveTo(xy[0], xy[1])
        for (i in 2 until xy.size step 2) lineTo(xy[i], xy[i + 1])
        close()
    }

    fun polyline(xy: FloatArray): Path = Path().apply {
        moveTo(xy[0], xy[1])
        for (i in 2 until xy.size step 2) lineTo(xy[i], xy[i + 1])
    }
}

/** A gentle back-and-forth for the leaf, in degrees. */
@Composable
internal fun rememberLeafSway(amplitude: Float = 4f, periodMs: Int = 2600): Float =
    rememberLeafSwayState(amplitude, periodMs).value

/**
 * The same sway, to be read while drawing (not while building the screen), so that only the logo
 * is redrawn on each frame and the screen around it is left alone.
 */
@Composable
internal fun rememberLeafSwayState(amplitude: Float = 4f, periodMs: Int = 2600): State<Float> {
    val loop = rememberInfiniteTransition(label = "leaf")
    return loop.animateFloat(
        initialValue = -amplitude,
        targetValue = amplitude,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
}

/**
 * The KarmaKitchen wordmark: maroon "Karma" with the half-Hindi K and its leaf, green "Kitchen".
 * In dark mode it turns cream and light green so it stays readable. [height] is the full height,
 * from the tip of the leaf to the bottom of the K.
 */
@Composable
fun KarmaKitchenWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    karmaColor: Color = LocalPalette.current.wordmarkKarma,
    kitchenColor: Color = LocalPalette.current.wordmarkKitchen,
    swayLeaf: Boolean = false,
) {
    val sway = if (swayLeaf) rememberLeafSwayState(amplitude = 3f) else null
    val aspect = (BrandArt.WM_RIGHT - BrandArt.WM_LEFT) / (BrandArt.WM_BOTTOM - BrandArt.WM_TOP)
    Canvas(
        modifier
            .height(height)
            .aspectRatio(aspect)
            .semantics { contentDescription = "KarmaKitchen" }
    ) {
        val s = size.height / (BrandArt.WM_BOTTOM - BrandArt.WM_TOP)
        withTransform({
            scale(s, s, pivot = Offset.Zero)
            translate(-BrandArt.WM_LEFT, -BrandArt.WM_TOP)
        }) {
            drawWordmark(karmaColor, kitchenColor, sway?.value ?: 0f)
        }
    }
}

/** Draws the wordmark in wordmark coordinates. */
internal fun DrawScope.drawWordmark(karma: Color, kitchen: Color, leafSway: Float = 0f) {
    if (leafSway == 0f) {
        drawPath(BrandPaths.wordmarkK, karma)
    } else {
        clipPath(BrandPaths.wordmarkLeafRegion, ClipOp.Difference) { drawPath(BrandPaths.wordmarkK, karma) }
        rotate(leafSway, pivot = Offset(262f, 339f)) {
            clipPath(BrandPaths.wordmarkLeafRegion) { drawPath(BrandPaths.wordmarkK, karma) }
        }
    }
    BrandPaths.arma.forEach { drawPath(it.second, karma) }
    BrandPaths.kitchen.forEach { drawPath(it.second, kitchen) }
}

/** Draws the icon K in icon coordinates, with the leaf turned by [leafTurn] and grown to [leafScale]. */
internal fun DrawScope.drawIconK(color: Color, leafTurn: Float = 0f, leafScale: Float = 1f, alpha: Float = 1f) {
    clipPath(BrandPaths.bodyRegion) { drawPath(BrandPaths.iconK, color, alpha = alpha) }
    if (leafScale <= 0.001f) return
    val base = Offset(BrandArt.LEAF_BASE[0], BrandArt.LEAF_BASE[1])
    withTransform({
        rotate(leafTurn, pivot = base)
        scale(leafScale, leafScale, pivot = base)
    }) {
        clipPath(BrandPaths.leafRegion) { drawPath(BrandPaths.iconK, color, alpha = alpha) }
    }
}

/**
 * The app-icon mark: the cream half-Hindi K with its leaf on a maroon rounded square, exactly like
 * the launcher icon. [leafTurn] tilts the leaf (in degrees) for small animations.
 */
@Composable
fun KarmaKitchenMark(modifier: Modifier = Modifier, size: Dp = 96.dp, leafTurn: Float = 0f) {
    Canvas(modifier.size(size).semantics { contentDescription = "KarmaKitchen" }) {
        drawRoundRect(LogoMaroon, cornerRadius = CornerRadius(this.size.minDimension * 0.225f))
        // The K sits in a 384-unit square centred on (1383, 417), as in the icon artwork.
        val side = 384f
        val s = this.size.width / side
        withTransform({
            scale(s, s, pivot = Offset.Zero)
            translate(-(1383f - side / 2), -(417f - side / 2))
        }) {
            drawIconK(LogoCream, leafTurn = leafTurn)
        }
    }
}
