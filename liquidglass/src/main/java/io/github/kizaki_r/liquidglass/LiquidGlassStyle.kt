package io.github.kizaki_r.liquidglass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every tunable value of [LiquidGlassTabBar] in one place.
 *
 * Start from [LiquidGlassDefaults.style] and override what you need:
 * ```
 * val style = LiquidGlassDefaults.style(darkTheme).copy(
 *     lens = LiquidGlassLens(dispersion = 3.dp),
 *     motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
 * )
 * ```
 * All lengths are in dp **on screen at full press**; all spring stiffness values follow Compose's
 * `spring(dampingRatio, stiffness)` (higher stiffness = faster, damping ratio < 1 = overshoot).
 */
@Immutable
data class LiquidGlassStyle(
    val metrics: LiquidGlassMetrics = LiquidGlassMetrics(),
    val panel: LiquidGlassPanel = LiquidGlassPanel(),
    val lens: LiquidGlassLens = LiquidGlassLens(),
    val colors: LiquidGlassColors,
    val motion: LiquidGlassMotion = LiquidGlassMotion(),
)

/** Size and layout of the bar and the selection pill. */
@Immutable
data class LiquidGlassMetrics(
    /** Height of the glass bar. */
    val barHeight: Dp = 54.dp,
    /** Height of the selection pill at rest (the lifted lens is this times [LiquidGlassLens.liftScaleY]). */
    val pillHeight: Dp = 48.dp,
    /** Horizontal padding between the bar's edge and the first / last slot. */
    val contentPadding: Dp = 8.dp,
    /** Minimum gap between the pill and the bar's edge when the pill is at the first / last slot. */
    val pillInset: Dp = 3.dp,
    /** How much wider the pill is than one slot. */
    val pillExtraWidth: Dp = 10.dp,
    /** How far the lifted lens may reach past the bar's edge when dragged to either end. */
    val maxLensOverhang: Dp = 14.dp,
    /** How far the whole bar is pulled sideways (rubber band) when the finger goes past either end. */
    val maxRubberBand: Dp = 3.dp,
    /** How much wider the glass bar (and its invisible copy) grows while the lens is pressed. It scales uniformly, so it also gets a little taller. */
    val panelPressGrow: Dp = 12.dp,
)

/** The frosted glass of the bar itself (not the lens). */
@Immutable
data class LiquidGlassPanel(
    /** Blur of the content behind the bar. */
    val blur: Dp = 16.dp,
    /** Width of the refracting rim along the bar's edge. */
    val rimWidth: Dp = 12.dp,
    /** How strongly that rim bends the content behind it. */
    val rimRefraction: Dp = 20.dp,
)

/** The lifted lens (the selection pill while it is pressed). */
@Immutable
data class LiquidGlassLens(
    /** Width of the lifted lens relative to the resting pill. */
    val liftScaleX: Float = 1.42f,
    /** Height of the lifted lens relative to the resting pill. */
    val liftScaleY: Float = 1.40f,
    /** Size of the bar's panel as seen inside the lens, relative to the bar outside it (shrunk about the lens center). */
    val contentScale: Float = 0.92f,
    /** Size of the tab icons / labels as seen inside the lens, relative to outside. What you actually see is this times [contentScale] (about 1.15x with the defaults). */
    val tabLiftScale: Float = 1.25f,
    /** Width of the refracting band along the lens edge (measured inward from the edge). */
    val rimWidth: Dp = 10.dp,
    /**
     * How far inward the outermost rim pixel samples. Keep it well below ~8 dp: deeper than that it samples the
     * bar's own panel instead of the content behind it, and the light along the rim looks "fixed".
     */
    val rimRefraction: Dp = 6.dp,
    /** Width of the color-fringe band along the lens edge (fades out over its inner 40%). */
    val dispersionWidth: Dp = 10.dp,
    /** Largest red-to-blue split of the color fringe, strongest toward the ends. 0 = no color fringe. */
    val dispersion: Dp = 4.5.dp,
    /** Blur radius of the inner shadow along the lens edge. */
    val innerShadowRadius: Dp = 9.dp,
    /** Vertical offset of that inner shadow (a top-lit bevel). */
    val innerShadowOffsetY: Dp = 6.dp,
    /** Opacity of that inner shadow. */
    val innerShadowAlpha: Float = 0.6f,
)

/** Colors and the theme-dependent opacity of the glass. Use [LiquidGlassDefaults.colors]. */
@Immutable
data class LiquidGlassColors(
    /** Color (with alpha) of the bar's glass surface. */
    val panelColor: Color,
    /** Color of the gray selection pill at rest. */
    val restingPillColor: Color,
    /** Blur of the bar as seen inside the lens; lower than the panel blur shows more of the content behind. */
    val lensSeenBlur: Dp,
    /** Multiplier on the panel's opacity as seen inside the lens; lower = the lens is more see-through. */
    val lensSeenSurfaceAlpha: Float,
    /** Strength of the soft white glow that follows the finger while pressed (0 = off, 1 = full). */
    val pressGlow: Float,
)

/** A spring: Compose's `spring(dampingRatio, stiffness)`. */
@Immutable
data class SpringConfig(val dampingRatio: Float, val stiffness: Float)

/** Timing and feel of the animations. */
@Immutable
data class LiquidGlassMotion(
    /** The pill sliding to the tapped tab. Lower stiffness = slower, more visible glide. */
    val tapSlide: SpringConfig = SpringConfig(0.85f, 450f),
    /** The pill following the finger while dragging. */
    val follow: SpringConfig = SpringConfig(1f, 1000f),
    /** The glass effects (refraction, highlight, shadow) fading in when pressed. */
    val pressGlow: SpringConfig = SpringConfig(1f, 1000f),
    /** The lens growing wider when pressed. */
    val liftX: SpringConfig = SpringConfig(0.6f, 250f),
    /** The lens growing taller when pressed. */
    val liftY: SpringConfig = SpringConfig(0.7f, 250f),
    /** The glass effects fading out on release. */
    val releaseGlow: SpringConfig = SpringConfig(1f, 700f),
    /** The lens shrinking back to the resting pill on release. */
    val releaseScale: SpringConfig = SpringConfig(0.8f, 550f),
    /** Smoothing of the finger velocity that drives the jelly stretch. */
    val velocity: SpringConfig = SpringConfig(0.5f, 300f),
    /** The bar settling back after being pulled past an end. */
    val rubberBand: SpringConfig = SpringConfig(0.6f, 400f),
    /** Horizontal stretch per unit of velocity (larger = more jelly). */
    val jellyStretchX: Float = 0.75f,
    /** Vertical squash per unit of velocity. */
    val jellyStretchY: Float = 0.25f,
    /** Largest stretch / squash fraction (0.2 = up to about +25% wide). */
    val jellyLimit: Float = 0.2f,
)

/** Ready-made styles. */
object LiquidGlassDefaults {

    /** Colors for a light or a dark app theme. */
    fun colors(darkTheme: Boolean): LiquidGlassColors =
        if (darkTheme) {
            LiquidGlassColors(
                panelColor = Color(0xFF0C0C0E).copy(alpha = 0.9f),
                restingPillColor = Color.White.copy(alpha = 0.1f),
                lensSeenBlur = 16.dp,
                lensSeenSurfaceAlpha = 1f,
                pressGlow = 0f,
            )
        } else {
            LiquidGlassColors(
                panelColor = Color(0xFFFAFAFA).copy(alpha = 0.8f),
                restingPillColor = Color.Black.copy(alpha = 0.08f),
                lensSeenBlur = 6.dp,
                lensSeenSurfaceAlpha = 0.68f,
                pressGlow = 1f,
            )
        }

    /** The default style for a light or a dark app theme. */
    fun style(darkTheme: Boolean): LiquidGlassStyle = LiquidGlassStyle(colors = colors(darkTheme))
}
