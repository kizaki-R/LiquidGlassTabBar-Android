package io.github.kizaki_r.liquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Wraps your screen so [LiquidGlassTabBar] can refract what is behind it. Put your real screen in [content] and
 * the bar in [bar]; no other setup is needed.
 *
 * ```
 * LiquidGlassScaffold(
 *     bar = {
 *         LiquidGlassTabBar(
 *             selectedIndex = selected, onTabSelected = { selected = it }, tabsCount = 4,
 *             modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp),
 *         ) { ... LiquidGlassTab(onClick = { ... }) { Icon(...) } ... }
 *     },
 * ) {
 *     MyScreen()   // draw edge to edge: it must extend under the bar
 * }
 * ```
 *
 * [content] is drawn first and recorded; [bar] is drawn on top, so the bar is never part of what it refracts.
 */
@Composable
fun LiquidGlassScaffold(
    modifier: Modifier = Modifier,
    bar: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(Modifier.layerBackdrop(backdrop).fillMaxSize()) { content() }
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides backdrop) {
            bar()
        }
    }
}
