// Adapted from Kyant0/AndroidLiquidGlass catalog sample (Apache License 2.0), see NOTICE.
package io.github.kizaki_r.liquidglass

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drives the selection pill: position (spring-follows the finger), press
 * progress (0 = resting, 1 = lifted glass), squash/stretch scale, and a
 * smoothed velocity used to deform the pill while it moves.
 */
internal class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    private val motion: LiquidGlassMotion,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScaleX: Float,
    val pressedScaleY: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private fun SpringConfig.toSpec(threshold: Float) = spring<Float>(dampingRatio, stiffness, threshold)

    // Following the finger: stiff, so the pill sticks to it.
    private val valueAnimationSpec = motion.follow.toSpec(visibilityThreshold)
    // Tap on a tab: a softer spring so the pill visibly glides across (e.g. 1st -> 4th tab). Stiffness sets the
    // duration (lower = slower), damping < 1 adds a hair of overshoot.
    private val tapSlideSpec = motion.tapSlide.toSpec(visibilityThreshold)
    private val velocityAnimationSpec = motion.velocity.toSpec(visibilityThreshold * 10f)
    // Lift: soft and slightly jelly-like.
    private val pressProgressAnimationSpec = motion.pressGlow.toSpec(0.001f)
    private val scaleXAnimationSpec = motion.liftX.toSpec(0.001f)
    private val scaleYAnimationSpec = motion.liftY.toSpec(0.001f)

    // Release: the glass fades out and the lens settles back into the bar with almost no bounce.
    private val releaseProgressAnimationSpec = motion.releaseGlow.toSpec(0.001f)
    private val releaseScaleAnimationSpec = motion.releaseScale.toSpec(0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    // Bumped when the pill is released/settles. A velocity animation queued before that (updateVelocity runs
    // every frame and launches asynchronously) must not start afterwards, or it would land after the reset
    // to 0 and leave a stale velocity. Do NOT cancel the previous per-frame animation: it only ever gets one
    // frame before the next call, so cancelling it stops the velocity (and the jelly stretch) from moving at all.
    private var velocityGeneration = 0

    /** True while the current gesture is a plain tap (not dragged): slide with [tapSlideSpec]. */
    var softSlide = false

    private val slideSpec get() = if (softSlide) tapSlideSpec else valueAnimationSpec

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { _, dragAmount ->
            onDrag(size, dragAmount)
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScaleX, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScaleY, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                // Keep the glass lifted until the pill has almost arrived.
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, releaseProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, releaseScaleAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, releaseScaleAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, slideSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                velocityGeneration++
                if (!softSlide) {
                    launch {
                        velocityAnimation.animateTo(0f, velocityAnimationSpec)
                    }
                }
                launch {
                    // A tap keeps feeding the velocity so the pill stretches while it glides, then settles to 0.
                    valueAnimation.animateTo(targetValue, slideSpec) { if (softSlide) updateVelocity() }
                    // Settled: whatever velocity was queued in the meantime, end at rest.
                    velocityGeneration++
                    velocityAnimation.animateTo(0f, velocityAnimationSpec)
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            SystemClock.uptimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        val generation = velocityGeneration
        animationScope.launch {
            if (generation == velocityGeneration) {
                velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
            }
        }
    }
}
