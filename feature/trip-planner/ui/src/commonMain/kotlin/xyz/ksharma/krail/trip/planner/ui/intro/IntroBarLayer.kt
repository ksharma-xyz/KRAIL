package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints

/**
 * What the decision bar takes of the screen, which the frame of each Intro screen reads. [reservedPx] is the height of
 * the bar's slot, which is there whether or not the bar is [shown], so a screen that centres its middle zone keeps it
 * where it is when the bar appears. [shown] is whether the rider can see the bar. Only a bar that is shown gets room at
 * the end of the scroll, because only then is it over the last of the content.
 */
internal data class IntroBarInset(val reservedPx: Int, val shown: Boolean) {
    /** The room a scrolling middle zone leaves at its end so its last row can scroll clear of the bar: none while hidden. */
    val scrollEndPx: Int get() = if (shown) reservedPx else 0
}

/** No bar over the content: a frame drawn outside the Intro shell, such as in a preview, or a screen that has none. */
internal val LocalIntroBarInset = compositionLocalOf { IntroBarInset(reservedPx = 0, shown = false) }

/**
 * The content of the Intro over the full height, and the decision bar laid over its bottom edge. The bar is measured
 * first, so [content] knows its height in the same pass through [LocalIntroBarInset] and never draws a frame with the
 * wrong room. The content is not shortened by the bar: nothing is clipped at the bar's top, and what scrolls goes edge
 * to edge down to the bottom of the layer. Without a [bar] the content has the whole layer.
 */
@Composable
internal fun IntroBarLayer(
    barShown: Boolean,
    modifier: Modifier = Modifier,
    bar: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val barPlaceable = bar?.let {
            subcompose(IntroBarLayerSlot.Bar, it).map { measurable ->
                measurable.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = height))
            }
        }.orEmpty()
        val barHeight = barPlaceable.maxOfOrNull { it.height } ?: 0
        val contentPlaceable = subcompose(IntroBarLayerSlot.Content) {
            CompositionLocalProvider(LocalIntroBarInset provides IntroBarInset(barHeight, barShown && bar != null)) {
                content()
            }
        }.map { it.measure(Constraints.fixed(width, height)) }
        layout(width, height) {
            contentPlaceable.forEach { it.place(0, 0) }
            barPlaceable.forEach { it.place(0, height - barHeight) }
        }
    }
}

private enum class IntroBarLayerSlot { Bar, Content }
