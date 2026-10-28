# Parameter reference

Language: English · [繁體中文](PARAMETERS.zh-TW.md) · [简体中文](PARAMETERS.zh-CN.md)

Every tunable value of `LiquidGlassTabBar`, generated from `LiquidGlassStyle.kt`. Lengths are dp on screen at full press. Start from `LiquidGlassDefaults.style(darkTheme)` and override what you need with `copy(...)`.

## Metrics (`metrics`)

Size and layout of the bar and the selection pill.

| Parameter | Default | What it does |
|---|---|---|
| `barHeight` | `54.dp` | Height of the glass bar. |
| `pillHeight` | `48.dp` | Height of the selection pill at rest (the lifted lens is this times `LiquidGlassLens.liftScaleY`). |
| `contentPadding` | `8.dp` | Horizontal padding between the bar's edge and the first / last slot. |
| `pillInset` | `3.dp` | Minimum gap between the pill and the bar's edge when the pill is at the first / last slot. |
| `pillExtraWidth` | `10.dp` | How much wider the pill is than one slot. |
| `maxLensOverhang` | `14.dp` | How far the lifted lens may reach past the bar's edge when dragged to either end. |
| `maxRubberBand` | `3.dp` | How far the whole bar is pulled sideways (rubber band) when the finger goes past either end. |
| `panelPressGrow` | `12.dp` | How much wider the glass bar (and its invisible copy) grows while the lens is pressed. It scales uniformly, so it also gets a little taller. |

## Panel (`panel`)

The frosted glass of the bar itself, not the lens.

| Parameter | Default | What it does |
|---|---|---|
| `blur` | `16.dp` | Blur of the content behind the bar. |
| `rimWidth` | `12.dp` | Width of the refracting rim along the bar's edge. |
| `rimRefraction` | `20.dp` | How strongly that rim bends the content behind it. |

## Lens (`lens`)

The lifted lens: the selection pill while it is pressed.

| Parameter | Default | What it does |
|---|---|---|
| `liftScaleX` | `1.42f` | Width of the lifted lens relative to the resting pill. |
| `liftScaleY` | `1.40f` | Height of the lifted lens relative to the resting pill. |
| `contentScale` | `0.92f` | Size of the bar's panel as seen inside the lens, relative to the bar outside it (shrunk about the lens center). |
| `tabLiftScale` | `1.25f` | Size of the tab icons / labels as seen inside the lens, relative to outside. What you actually see is this times `contentScale` (about 1.15x with the defaults). |
| `rimWidth` | `10.dp` | Width of the refracting band along the lens edge (measured inward from the edge). |
| `rimRefraction` | `6.dp` | How far inward the outermost rim pixel samples. Keep it well below ~8 dp: deeper than that it samples the bar's own panel instead of the content behind it, and the light along the rim looks "fixed". |
| `dispersionWidth` | `10.dp` | Width of the color-fringe band along the lens edge (fades out over its inner 40%). |
| `dispersion` | `4.5.dp` | Largest red-to-blue split of the color fringe, strongest toward the ends. 0 = no color fringe. |
| `innerShadowRadius` | `9.dp` | Blur radius of the inner shadow along the lens edge. |
| `innerShadowOffsetY` | `6.dp` | Vertical offset of that inner shadow (a top-lit bevel). |
| `innerShadowAlpha` | `0.6f` | Opacity of that inner shadow. |

## Colors (`colors`)

Colors and the theme-dependent opacity of the glass. The defaults differ for light and dark.

| Parameter | Light | Dark | What it does |
|---|---|---|---|
| `panelColor` | `#FAFAFA` @ 0.8 | `#0C0C0E` @ 0.9 | Color (with alpha) of the bar's glass surface. |
| `restingPillColor` | `black` @ 0.08 | `white` @ 0.1 | Color of the gray selection pill at rest. |
| `lensSeenBlur` | `6.dp` | `16.dp` | Blur of the bar as seen inside the lens; lower than the panel blur shows more of the content behind. |
| `lensSeenSurfaceAlpha` | `0.68f` | `1f` | Multiplier on the panel's opacity as seen inside the lens; lower = the lens is more see-through. |
| `pressGlow` | `1f` | `0f` | Strength of the soft white glow that follows the finger while pressed (0 = off, 1 = full). |

## Motion (`motion`)

Timing and feel. Every spring is `SpringConfig(dampingRatio, stiffness)`: higher stiffness is faster, a damping ratio below 1 overshoots.

| Parameter | Default | What it does |
|---|---|---|
| `tapSlide` | `SpringConfig(0.85f, 450f)` | The pill sliding to the tapped tab. Lower stiffness = slower, more visible glide. |
| `follow` | `SpringConfig(1f, 1000f)` | The pill following the finger while dragging. |
| `pressGlow` | `SpringConfig(1f, 1000f)` | The glass effects (refraction, highlight, shadow) fading in when pressed. |
| `liftX` | `SpringConfig(0.6f, 250f)` | The lens growing wider when pressed. |
| `liftY` | `SpringConfig(0.7f, 250f)` | The lens growing taller when pressed. |
| `releaseGlow` | `SpringConfig(1f, 700f)` | The glass effects fading out on release. |
| `releaseScale` | `SpringConfig(0.8f, 550f)` | The lens shrinking back to the resting pill on release. |
| `velocity` | `SpringConfig(0.5f, 300f)` | Smoothing of the finger velocity that drives the jelly stretch. |
| `rubberBand` | `SpringConfig(0.6f, 400f)` | The bar settling back after being pulled past an end. |
| `jellyStretchX` | `0.75f` | Horizontal stretch per unit of velocity (larger = more jelly). |
| `jellyStretchY` | `0.25f` | Vertical squash per unit of velocity. |
| `jellyLimit` | `0.2f` | Largest stretch / squash fraction (0.2 = up to about +25% wide). |

## Example

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)
LiquidGlassTabBar(/* ... */ style = style) { /* ... */ }
```
