# LiquidGlassTabBar-Android

English · [繁體中文](README.zh-TW.md) · [简体中文](README.zh-CN.md)

A bottom tab bar for Jetpack Compose in which the selected pill becomes a glass lens while you press it. Hold the pill and it lifts, blurs and bends whatever is behind the bar, gets a thin color fringe along its rim, and stretches a little in the direction you drag. Let go and it settles on the nearest tab. A plain tap makes the pill glide across to the new tab.

The lens is drawn by an AGSL shader, so it needs Android 13 (API 33). The numbers behind the look and the motion are the 39 fields of one `LiquidGlassStyle` object, listed in [docs/PARAMETERS.md](docs/PARAMETERS.md).

## Requirements

This is what the library was built and run with. No older tool version has been tried.

| | Version |
|---|---|
| Android | `minSdk` 24, lens effect on API 33+; run on API 36 (phone) and API 32 to 35 (emulator) |
| Kotlin | 2.4.10 |
| Compose | BOM 2026.09.00 (Compose 1.12) |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.5.0 |
| JDK | 17 bytecode, built on 21 |
| `compileSdk` | 37 |

The blur and refraction come from [Kyant0's backdrop library](https://github.com/Kyant0/AndroidLiquidGlass) 2.0.1, which is pulled in for you. That release is itself built against Compose 1.12 and Kotlin 2.4.10, so a project on older versions may fail to resolve.

## Install

From JitPack:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.kizaki-R:LiquidGlassTabBar-Android:v0.2.0")
}
```

JitPack shows the exact line for any tag at <https://jitpack.io/#kizaki-R/LiquidGlassTabBar-Android>.

Or from source: copy the `liquidglass` folder into your project (a git submodule works too) and include it. If Gradle cannot find the `com.android.library` plugin, declare it in the root build file next to your application plugin, with your own AGP version and `apply false`.

```kotlin
// settings.gradle.kts
include(":liquidglass")

// app/build.gradle.kts
dependencies {
    implementation(project(":liquidglass"))
}
```

## Quick start

```kotlin
@Composable
fun App() {
    var selected by remember { mutableIntStateOf(0) }
    val tint = if (isSystemInDarkTheme()) Color.White else Color.Black

    LiquidGlassScaffold(
        bar = {
            LiquidGlassTabBar(
                selectedIndex = selected,
                onTabSelected = { selected = it },
                tabsCount = 4,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                listOf(Icons.Outlined.Home, Icons.Outlined.Search, Icons.Outlined.Favorite, Icons.Outlined.Person)
                    .forEachIndexed { index, icon ->
                        LiquidGlassTab(onClick = { selected = index }) {
                            Icon(icon, contentDescription = null, tint = tint)
                        }
                    }
            }
        },
    ) {
        MyScreen(selected) // draw edge to edge: it has to extend under the bar
    }
}
```

The imports are `io.github.kizaki_r.liquidglass.LiquidGlassScaffold`, `LiquidGlassTabBar` and `LiquidGlassTab`. The example uses Material 3's `Icon` and the icons from `androidx.compose.material:material-icons-core`; Material 3 does not expose that artifact, so add it if you copy the example. Any composable can sit inside a tab.

The scaffold records your screen so the bar has something to refract; that one wrapper is the whole setup. Two rules go with it. Put your real screen in the scaffold's content and let it run under the bar, because a screen that stops above the bar leaves the glass nothing to bend. And keep the bar in the `bar` slot, outside the content, or it would refract itself.

`tabsCount` is the number of slots in the bar. Each slot must take `Modifier.weight(1f)`, which `LiquidGlassTab` already does.

### A center button that is not a tab

Mark the slot with `isSelectable`. The pill skips it and the slot keeps its own click.

```kotlin
LiquidGlassTabBar(
    selectedIndex = selected,
    onTabSelected = { selected = it },
    tabsCount = 5,
    isSelectable = { it != 2 },
) {
    LiquidGlassTab(onClick = { selected = 0 }) { Icon(Icons.Outlined.Home, null, tint = tint) }
    LiquidGlassTab(onClick = { selected = 1 }) { Icon(Icons.Outlined.Search, null, tint = tint) }
    Box(
        Modifier.weight(1f).fillMaxHeight().clickable { /* open the composer */ },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Add, contentDescription = "New", tint = tint) }
    LiquidGlassTab(onClick = { selected = 3 }) { Icon(Icons.Outlined.Favorite, null, tint = tint) }
    LiquidGlassTab(onClick = { selected = 4 }) { Icon(Icons.Outlined.Person, null, tint = tint) }
}
```

## Changing the look

Start from the default style for your theme and override what you need.

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)

LiquidGlassTabBar(/* ... */ darkTheme = darkTheme, style = style) { /* ... */ }
```

Pass your app's own theme as `darkTheme`; it defaults to the system's, which is wrong if your app has an in-app switch. A style holds five groups: `metrics`, `panel`, `lens`, `colors` and `motion`. Building a group yourself, as `LiquidGlassLens(dispersion = 3.dp)` does above, sets every other field of that group to its default. `colors` has no defaults of its own; start from `LiquidGlassDefaults.colors(darkTheme).copy(...)`.

The ones you are most likely to reach for:

| To change | Field | Default |
|---|---|---|
| Lens width and height | `lens.liftScaleX`, `lens.liftScaleY` | `1.42f`, `1.40f` |
| Color fringe | `lens.dispersion` | `4.5.dp` (`0.dp` turns it off) |
| How far the rim bends the content | `lens.rimRefraction` | `6.dp` |
| Tap glide speed | `motion.tapSlide` | `SpringConfig(0.85f, 450f)`, lower stiffness is slower |
| Glass fade-out on release | `motion.releaseGlow` | `SpringConfig(1f, 700f)` |
| Jelly stretch | `motion.jellyStretchX`, `motion.jellyLimit` | `0.75f`, `0.2f` |
| Bar color and opacity | `colors.panelColor` | light `#FAFAFA` at 0.8, dark `#0C0C0E` at 0.9 |
| Bar height | `metrics.barHeight` | `54.dp` |

All 39 fields, with the light and dark defaults, are in [docs/PARAMETERS.md](docs/PARAMETERS.md).

## Using it with an AI coding assistant

The `skills/liquid-glass-tab-bar` folder is a Claude Code skill. Copy it into your project's `.claude/skills/` (or `~/.claude/skills/`) and the assistant knows how to add the bar, which fields change which part of the look, and the mistakes to avoid.

## How it works

The bar is three layers.

1. The glass panel. A blurred, slightly refracted copy of the content behind the bar, with your tabs drawn on top.
2. A hidden copy of the panel and the tabs, recorded as a backdrop of its own. Nothing shows it except the lens.
3. The pill. At rest it is a gray capsule. When pressed it scales up to 1.42 × 1.40 of its size and looks through to the page and to the hidden copy. The copy is shrunk to 0.92 about the lens center, so around 8 dp of the real screen shows between the lens edge and the panel's edge.

Those 8 dp decide how deep the rim may sample. The backdrop library's own lens samples roughly 9 to 17 dp inward on screen, which always lands on the panel copy, so the light along the rim never reacted to the content behind it. The lens here is our own shader, adapted from the library's, and the default `lens.rimRefraction` of 6 dp stays inside it. Past about 8 dp it falls back to that fixed look.

The color fringe has its own width and strength in the shader and no longer depends on the refraction, because the refraction is close to zero where the panel's bright edge sits.

## Limits

- Android only. The lens needs API 33. On API 32 the code skips the lens shader and the pill is drawn as a plain translucent capsule without refraction or color fringe; that was run on an emulator in the light theme only. Below API 32 has not been run.
- Run on one phone, Android 16 (API 36), and on x86_64 emulators for API 32 to 35. The emulators use software rendering, so they say nothing about frame rate. Low-end devices have not been profiled.
- The fringe is thin, and is easiest to see where the rim crosses a sharp edge in the content. Over flat color it is faint.
- Right-to-left layouts are handled in the code and have not been tried.
- Equal-width slots in one row. Badges, labels and similar belong in the tab content you pass in.

## Credits and license

Built on [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) by Kyant0 (backdrop 2.0.1, shapes 1.2.1). The drag animation, the interactive highlight, the drag gesture inspector and the refraction math of the lens shader are adapted from that project, which is Apache-2.0; [NOTICE](NOTICE) lists them.

Licensed under the [Apache License 2.0](LICENSE).
