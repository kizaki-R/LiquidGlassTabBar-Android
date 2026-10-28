// Based on LiquidBottomTabs from Kyant0/AndroidLiquidGlass catalog sample (Apache License 2.0), see NOTICE.
// Extended with non-selectable slots (e.g. a center "+" button), press-anywhere scrubbing where the pill
// tracks the finger 1:1, a shrunken copy of the bar inside the lifted lens, and our own lens shader.
package io.github.kizaki_r.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * The content the bar should refract. [LiquidGlassScaffold] provides it; supply it yourself only if you build
 * the layout by hand with `Modifier.layerBackdrop(backdrop)`.
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

internal val LocalGlassTabScale =
    staticCompositionLocalOf { { 1f } }

/** Non-null only inside the accent copy; tabs tint themselves, other slots (e.g. a "+") stay as-is. */
internal val LocalGlassTabTint =
    staticCompositionLocalOf<Color?> { null }

/**
 * Draws [inner] scaled by [contentScale] about the center of the node that draws it (the pill).
 * The pill's own layerBlock scale is also about its center and the backdrop library already draws backdrops with
 * the inverse of that transform, so both are plain scales about the same point and commute: on screen
 * the content shrinks about the lens center. Only wraps the tabs layer, so the page behind the bar is
 * not shrunk.
 */
private class ShrunkBackdrop(
    private val inner: Backdrop,
    private val contentScale: () -> Float
) : Backdrop {
    override val isCoordinatesDependent: Boolean get() = inner.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val s = contentScale()
        if (s == 1f || coordinates == null) {
            with(inner) { drawBackdrop(density, coordinates, layerBlock) }
            return
        }
        // Pill-local, unscaled layout size. Not this.size: DrawBackdropNode records into a layer
        // padded by the effect padding and has already translated the canvas by that padding.
        val pivot = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
        scale(s, s, pivot) {
            with(inner) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}

/**
 * Liquid glass bottom bar.
 *
 * Layers, bottom to top:
 * 1. Glass panel: blur + lens refraction (with dispersion) of [backdrop], tabs drawn on it.
 * 2. Invisible accent copy of the panel + tabs, captured into its own backdrop layer.
 * 3. Selection pill: while pressed it lifts into a lens that refracts (page + accent copy), so the selected tab
 *    shows in [accentColor]; it squashes / stretches with finger velocity.
 *
 * Touch: press anywhere on a selectable slot and the pill jumps under the finger and follows it;
 * on release it settles on the nearest selectable slot. A tap is just a very short scrub.
 *
 * Put [LiquidGlassTab]s (and any non-selectable slots) in [content]; each slot must take `Modifier.weight(1f)`.
 *
 * @param selectedIndex index of the selected slot.
 * @param onTabSelected called when the user selects a slot.
 * @param tabsCount number of slots in [content], including non-selectable ones.
 * @param backdrop the content to refract; defaults to the one provided by [LiquidGlassScaffold].
 * @param darkTheme picks the default [style]; pass your app's theme, not necessarily the system's.
 * @param style every tunable value, see [LiquidGlassStyle].
 * @param accentColor color of the tabs as seen inside the lens (use your normal icon color for no color change).
 * @param isSelectable slots that return false (e.g. a center action button) are skipped by the pill
 *   and keep their own click handling.
 */
@Composable
fun LiquidGlassTabBar(
    selectedIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalLiquidGlassBackdrop.current
        ?: error("LiquidGlassTabBar needs a backdrop: put it inside LiquidGlassScaffold's `bar` slot or pass `backdrop`."),
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: LiquidGlassStyle = LiquidGlassDefaults.style(darkTheme),
    accentColor: Color = if (darkTheme) Color.White else Color.Black,
    isSelectable: (index: Int) -> Boolean = { true },
    content: @Composable RowScope.() -> Unit
) {
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val selectedTabIndex: () -> Int = { currentSelectedIndex }

    val m = style.metrics
    val panel = style.panel
    val lensStyle = style.lens
    val colors = style.colors
    val motion = style.motion
    val containerColor = colors.panelColor
    val currentIsSelectable by rememberUpdatedState(isSelectable)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val tabsBackdrop = rememberLayerBackdrop()
    val animationScope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }

    // Rubber-band overshoot (px) of the whole panel when the finger goes past either end.
    val offsetAnimation = remember { Animatable(0f) }

    val dampedDragAnimation = remember(animationScope, motion, lensStyle.liftScaleX, lensStyle.liftScaleY) {
        DampedDragAnimation(
            animationScope = animationScope,
            motion = motion,
            initialValue = selectedTabIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScaleX = lensStyle.liftScaleX,
            pressedScaleY = lensStyle.liftScaleY,
            // Gestures are handled by scrubModifier below.
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> }
        )
    }
    val lensTabsBackdrop = remember(tabsBackdrop, dampedDragAnimation, lensStyle.contentScale) {
        ShrunkBackdrop(tabsBackdrop) {
            lerp(1f, lensStyle.contentScale, dampedDragAnimation.pressProgress)
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { selectedTabIndex() }
            .collectLatest { index -> currentIndex = index }
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDragAnimation.animateToValue(index.toFloat())
                currentOnTabSelected(index)
            }
    }

    fun nearestSelectable(from: Int, preferredDirection: Int): Int {
        if (currentIsSelectable(from)) return from
        for (step in 1 until tabsCount) {
            for (dir in intArrayOf(preferredDirection, -preferredDirection)) {
                val candidate = from + dir * step
                if (candidate in 0 until tabsCount && currentIsSelectable(candidate)) {
                    return candidate
                }
            }
        }
        return currentIndex
    }

    // Attached to the un-transformed outer box, so finger positions map straight to slots.
    val scrubModifier = Modifier.pointerInput(tabsCount, isLtr, m.contentPadding, motion) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val inset = m.contentPadding.toPx()
            val tabWidth = (size.width - 2f * inset) / tabsCount
            val maxValue = (tabsCount - 1).toFloat()
            val direction = if (isLtr) 1f else -1f

            fun rawValueAt(x: Float): Float {
                val startX = if (isLtr) x else size.width - x
                return (startX - inset) / tabWidth - 0.5f
            }

            val startSlot = rawValueAt(down.position.x).fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
            if (!currentIsSelectable(startSlot)) return@awaitEachGesture

            var position = dampedDragAnimation.value
            fun follow(x: Float) {
                val raw = rawValueAt(x)
                position = raw.fastCoerceIn(0f, maxValue)
                dampedDragAnimation.updateValue(position)
                val overshoot = (raw - position) * tabWidth * direction
                animationScope.launch { offsetAnimation.snapTo(overshoot) }
            }

            // A tap slides with the soft spring; as soon as the finger moves past the touch slop it becomes a
            // drag and the pill follows with the stiff spring (set back to false below).
            var dragging = false
            dampedDragAnimation.softSlide = true
            dampedDragAnimation.press()
            follow(down.position.x)

            // Always settle, even if the gesture coroutine is cancelled mid-drag (system edge gesture,
            // focus loss, parent cancelling). Otherwise the pill would stay lifted / deformed.
            try {
                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                    if (!dragging && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                        dragging = true
                        dampedDragAnimation.softSlide = false
                    }
                    if (dragging) follow(change.position.x)
                    if (!change.pressed) break
                }
            } finally {
                val rounded = position.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                val target = nearestSelectable(rounded, if (position >= rounded) 1 else -1)
                currentIndex = target
                dampedDragAnimation.animateToValue(target.toFloat())
                dampedDragAnimation.release()
                animationScope.launch {
                    offsetAnimation.animateTo(
                        0f,
                        spring(motion.rubberBand.dampingRatio, motion.rubberBand.stiffness, 0.5f)
                    )
                }
            }
        }
    }

    // As an ancestor of every slot it sees all touches; a non-selectable slot still gets its own click.
    BoxWithConstraints(
        modifier.then(scrubModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val barWidth = constraints.maxWidth.toFloat()
        val contentPadding = with(density) { m.contentPadding.toPx() }
        val pillInset = with(density) { m.pillInset.toPx() }
        val tabWidth = (barWidth - 2f * contentPadding) / tabsCount
        val pillWidth = tabWidth + with(density) { m.pillExtraWidth.toPx() }

        // Pill left edge for a (fractional) slot value: centered on the slot, kept inside the panel.
        fun pillLeft(value: Float): Float {
            val center = contentPadding + (value + 0.5f) * tabWidth
            val left = (center - pillWidth / 2f).fastCoerceIn(pillInset, barWidth - pillInset - pillWidth)
            return if (isLtr) left else barWidth - left - pillWidth
        }

        // Lifted size, squashed/stretched by horizontal velocity (jelly).
        // Velocity only deforms the pill while it is pressed, so a stale velocity can never leave it stretched.
        fun pillScaleX(): Float {
            val velocity = dampedDragAnimation.velocity * dampedDragAnimation.pressProgress / 10f
            return dampedDragAnimation.scaleX /
                (1f - (velocity * motion.jellyStretchX).fastCoerceIn(-motion.jellyLimit, motion.jellyLimit))
        }
        fun pillScaleY(): Float {
            val velocity = dampedDragAnimation.velocity * dampedDragAnimation.pressProgress / 10f
            return dampedDragAnimation.scaleY *
                (1f - (velocity * motion.jellyStretchY).fastCoerceIn(-motion.jellyLimit, motion.jellyLimit))
        }

        val panelOffset by remember(density, m.maxRubberBand) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    m.maxRubberBand.toPx() * fraction.sign * EaseOut.transform((abs(fraction) * 4f).fastCoerceIn(0f, 1f))
                }
            }
        }

        val interactiveHighlight = remember(animationScope, colors.pressGlow) {
            InteractiveHighlight(
                animationScope = animationScope,
                strength = colors.pressGlow,
                position = { size, _ ->
                    Offset(
                        pillLeft(dampedDragAnimation.value) + pillWidth / 2f + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // 1. Glass panel with the visible tabs.
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(panel.blur.toPx())
                        // Narrow refracting rim with a little dispersion; the middle stays readable.
                        lens(
                            panel.rimWidth.toPx(),
                            panel.rimRefraction.toPx(),
                            chromaticAberration = true
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + m.panelPressGrow.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
                .height(m.barHeight)
                .fillMaxWidth()
                .padding(horizontal = m.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // 2. Invisible accent-tinted copy, only seen through the pill. Only tabs are tinted, so a "+" slot
        //    keeps its own colors instead of turning into a solid blob under the lens.
        CompositionLocalProvider(
            LocalGlassTabScale provides {
                lerp(1f, lensStyle.tabLiftScale, dampedDragAnimation.pressProgress)
            },
            LocalGlassTabTint provides accentColor
        ) {
            Box(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                        // Grow with the visible panel, so contentScale is relative to what is outside the lens.
                        val grow = lerp(1f, 1f + m.panelPressGrow.toPx() / size.width, dampedDragAnimation.pressProgress)
                        scaleX = grow
                        scaleY = grow
                    }
                    .height(m.barHeight)
                    .fillMaxWidth()
            ) {
                // Panel surface, then the tinted slots on top.
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(lerp(panel.blur.toPx(), colors.lensSeenBlur.toPx(), progress))
                                lens(
                                    panel.rimWidth.toPx() * progress,
                                    panel.rimRefraction.toPx() * progress
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(containerColor, alpha = lerp(1f, colors.lensSeenSurfaceAlpha, progress))
                            }
                        )
                        .then(interactiveHighlight.modifier)
                )
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = m.contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        // 3. Selection pill (purely visual; touches are handled by scrubModifier on the outer box).
        Box(
            Modifier
                .graphicsLayer {
                    // Keep the lifted (scaled) lens within maxLensOverhang of the panel edges.
                    val left = pillLeft(dampedDragAnimation.value) + panelOffset
                    val grow = pillWidth * (pillScaleX() - 1f) / 2f
                    val overhang = m.maxLensOverhang.toPx()
                    translationX = left
                        .coerceAtLeast(grow - overhang)
                        .coerceAtMost(barWidth + overhang - pillWidth - grow)
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, lensTabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        // Edge-only refraction + dispersion: content in the middle of the bubble is barely magnified.
                        // Effects run in the pill's unscaled space; the lifted pill is liftScaleX larger on screen.
                        glassLens(
                            refractionHeight = lensStyle.rimWidth.toPx() / lensStyle.liftScaleX * progress,
                            refractionAmount = lensStyle.rimRefraction.toPx() / lensStyle.liftScaleX * progress,
                            dispersionHeight = lensStyle.dispersionWidth.toPx() / lensStyle.liftScaleX * progress,
                            dispersionAmount = lensStyle.dispersion.toPx() / lensStyle.liftScaleX * progress
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    // Narrow top-lit bevel: darkens only the rim, the interior stays bright.
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = lensStyle.innerShadowRadius * progress,
                            offset = DpOffset(0.dp, lensStyle.innerShadowOffsetY * progress),
                            alpha = lensStyle.innerShadowAlpha * progress
                        )
                    },
                    layerBlock = {
                        scaleX = pillScaleX()
                        scaleY = pillScaleY()
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        // Resting: soft gray pill. Pressed: clear glass, no extra tint.
                        drawRect(colors.restingPillColor, alpha = 1f - progress)
                    }
                )
                .height(m.pillHeight)
                .width(with(density) { pillWidth.toDp() })
        )
    }
}

/** One selectable slot in [LiquidGlassTabBar]; grows with the pill while it is pressed. */
@Composable
fun RowScope.LiquidGlassTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalGlassTabScale.current
    val tint = LocalGlassTabTint.current
    val performClick = onClick
    Column(
        modifier
            // Touch is handled by the bar's scrub gesture; keep tabs reachable for accessibility.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.onClick(label = null, action = { performClick(); true })
            }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .then(
                if (tint != null) Modifier.graphicsLayer(colorFilter = ColorFilter.tint(tint))
                else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(1f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
