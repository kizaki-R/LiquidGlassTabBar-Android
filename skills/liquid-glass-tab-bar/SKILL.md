---
name: liquid-glass-tab-bar
description: Add the LiquidGlassTabBar-Android bottom tab bar (a draggable glass lens that refracts the content behind it, with a thin color fringe and jelly motion) to a Jetpack Compose app, and tune its size, refraction, color fringe, speed and colors. Use when the user asks for a liquid glass, glassmorphism or frosted bottom navigation bar, a draggable selection pill, or wants to change how an existing one looks or moves.
---

# LiquidGlassTabBar-Android

A Compose bottom bar. The selected pill becomes a glass lens while pressed: it lifts, blurs and bends what is behind the bar, shows a thin color fringe on its rim and stretches with finger speed. A tap glides the pill to the new tab. Android only.

Read `README.md` for the full picture and `docs/PARAMETERS.md` for every tunable field. This file is the short version for adding it and the mistakes that cost time.

## Before you start

Check the project, because the library was built and run only with these versions: Kotlin 2.4.10, Compose BOM 2026.09.00 (Compose 1.12), AGP 9.2.1, Gradle 9.5.0, `compileSdk` 37, `minSdk` 24. It was run on Android 16 (API 36). The lens shader needs API 33; below that the code skips it and that path is untested. Its dependency, Kyant0's backdrop 2.0.1, is built against Compose 1.12 and Kotlin 2.4.10, so an older Compose or Kotlin may not resolve. Tell the user if the project is older instead of working around it silently.

Ask where the library should come from if the user has not said: JitPack (`com.github.kizaki-R:LiquidGlassTabBar-Android:<tag>`, repository `https://jitpack.io`), or the `liquidglass` module copied into the project and included with `include(":liquidglass")`.

## Adding it

1. Add the dependency. If the example uses Material icons, `material-icons-core` is a separate artifact that Material 3 does not expose.
2. Wrap the existing screen in `LiquidGlassScaffold`. The screen goes in the content lambda and keeps its own background.
3. Put `LiquidGlassTabBar` in the `bar` slot, aligned to the bottom, with `navigationBarsPadding()` and horizontal padding (20 dp looks right).
4. Fill the bar with `LiquidGlassTab(onClick = { ... }) { /* icon, optional label */ }`, one per slot, in `RowScope` order.
5. Pass `selectedIndex`, `onTabSelected` and `tabsCount`. Pass `darkTheme` from the app's own theme state, not the system's, when the app has an in-app switch.
6. For a center action button that is not a tab, keep it in the row as a plain composable with `Modifier.weight(1f)` and set `isSelectable = { it != <index> }`. `tabsCount` still counts it.

```kotlin
LiquidGlassScaffold(
    bar = {
        LiquidGlassTabBar(
            selectedIndex = selected,
            onTabSelected = { selected = it },
            tabsCount = 4,
            darkTheme = darkTheme,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            items.forEachIndexed { i, item ->
                LiquidGlassTab(onClick = { selected = i }) { Icon(item.icon, null, tint = tint) }
            }
        }
    },
) { MyScreen() }
```

## Mistakes that cost time

- **The screen must run under the bar.** If content stops above the bar, or a `Scaffold` adds bottom padding to it, the glass has nothing to refract and looks like a flat gray pill. Draw edge to edge.
- **The bar is never inside the scaffold's content.** It would refract itself.
- **Every slot needs `Modifier.weight(1f)`.** `LiquidGlassTab` has it; a custom slot has to add it, or the pill lands in the wrong place.
- **`lens.rimRefraction` must stay well under about 8 dp.** Deeper than that the rim samples the bar's own panel instead of the content behind, and the light along the rim stops reacting to scrolling. It is the usual cause of "the glass looks fixed".
- **`colors` has no defaults.** Build it with `LiquidGlassDefaults.colors(darkTheme).copy(...)`, not `LiquidGlassColors(...)`.
- **Building a group such as `LiquidGlassLens(...)` resets its other fields to the library defaults**, not to whatever the base style had.
- **No lens below API 33.** Do not promise the effect on older devices.

## Tuning

Start from `LiquidGlassDefaults.style(darkTheme).copy(...)`. Map the user's wish to a field:

| The user says | Change | Default |
|---|---|---|
| lens too small / too big | `lens.liftScaleX`, `lens.liftScaleY` | 1.42, 1.40 |
| less or more rainbow edge | `lens.dispersion` (0 = off), `lens.dispersionWidth` | 4.5 dp, 10 dp |
| edge does not bend the background | `lens.rimRefraction`, `lens.rimWidth` | 6 dp, 10 dp |
| tap glide too fast or too slow | `motion.tapSlide` stiffness (lower is slower) | 450 |
| glass vanishes too abruptly on release | `motion.releaseGlow` stiffness (lower is slower) | 700 |
| lens shrinks back too fast or too slow | `motion.releaseScale` stiffness | 550 |
| more or less jelly stretch | `motion.jellyStretchX`, `motion.jellyLimit` | 0.75, 0.2 |
| bar too transparent or too opaque | `colors.panelColor` alpha | light 0.8, dark 0.9 |
| lens interior too clear or too frosted | `colors.lensSeenSurfaceAlpha`, `colors.lensSeenBlur` | light 0.68 and 6 dp, dark 1.0 and 16 dp |
| bar taller or shorter | `metrics.barHeight`, `metrics.pillHeight` | 54 dp, 48 dp |
| tabs inside the lens look too big or small | `lens.tabLiftScale`, `lens.contentScale` | 1.25, 0.92 |

Change one field at a time and look at the result before changing the next. Springs are `SpringConfig(dampingRatio, stiffness)`; a damping ratio under 1 overshoots.

## Checking the result

Build and run on a device or an API 33+ emulator. Put high-contrast content behind the bar (photos, a dark chart), press and hold the pill, drag it across the bar and take a screenshot while it is lifted. A correct result shows the content behind the bar bent at the lens rim and softened inside it, with the tab icons still crisp. A flat gray lens means the content is not running under the bar or the rim samples too deep. Check light and dark themes, and check that the resting bar looks the same as before the change.
