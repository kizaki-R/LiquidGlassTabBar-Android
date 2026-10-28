// Our own lens shader for the lifted selection pill. The refraction / dispersion math starts from
// Kyant0/AndroidLiquidGlass (backdrop 2.0.1, internal/Shaders.kt, Apache License 2.0) and is applied
// through the library's public BackdropEffectScope.runtimeShaderEffect, so the shader is cached per
// node and only uniforms change per frame.
package io.github.kizaki_r.liquidglass

import androidx.compose.ui.util.fastCoerceAtLeast
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.runtimeShaderEffect
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val GlassLensShader = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float radius;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float dispersionHeight;
uniform float dispersionAmount;

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;

    float depth = max(-sdRoundedRect(centeredCoord, halfSize, radius), 0.0);
    if (depth >= max(refractionHeight, dispersionHeight)) {
        return content.eval(coord);
    }

    // Refraction: the rim samples inward, strongest at the very edge (circular profile).
    float d = depth < refractionHeight ? circleMap(1.0 - depth / refractionHeight) * refractionAmount : 0.0;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius));
    float2 refractedCoord = coord + d * grad;

    // Dispersion: its own band and amount (not tied to d, which is ~0 where the panel's bright edge
    // sits, so the library math showed almost no color there). Full strength over the outer part of
    // the band, smooth fade to zero at dispersionHeight; sign/strength by x*y like the library, so the
    // color order flips between the left and right halves and vanishes at the very center.
    float band = 1.0 - smoothstep(dispersionHeight * 0.6, dispersionHeight, depth);
    float dispersionIntensity = band * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
    float2 dispersedCoord = grad * (dispersionAmount * dispersionIntensity);

    half4 color = half4(0.0);

    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;

    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;

    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;

    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;

    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;

    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;

    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;

    return color;
}"""

/**
 * Capsule lens for the lifted pill (the node's shape must be a Capsule). All lengths are in the
 * pill's local (unscaled) px. Does nothing at zero strength or where AGSL is unavailable (API < 33),
 * so the resting pill is untouched.
 */
internal fun BackdropEffectScope.glassLens(
    refractionHeight: Float,
    refractionAmount: Float,
    dispersionHeight: Float,
    dispersionAmount: Float
) {
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }
    val size = size
    val padding = padding
    runtimeShaderEffect("GlassLens", GlassLensShader, "content") {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("radius", size.minDimension / 2f)
        setFloatUniform("refractionHeight", refractionHeight)
        setFloatUniform("refractionAmount", -refractionAmount)
        setFloatUniform("dispersionHeight", dispersionHeight)
        setFloatUniform("dispersionAmount", -dispersionAmount)
    }
}
