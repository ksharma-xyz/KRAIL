package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * The one dark that floats over the map, the same in light and dark app modes.
 *
 * The map is always light, so everything that floats on it (the group card, its bars, the status chip, the
 * recents card, the mode button and its block) is a fixed dark container with fixed light content, and never
 * follows the app mode. taj already has the pair, `stopLabelSurface` and `onStopLabelSurface`, which are the
 * same dark and the same light in both modes, so [container] and [content] are those tokens and nothing is a
 * hex value or a new token.
 *
 * Rows and buttons that are not ours (the stop row, the label pills, the chips) read `onSurface`, `label` and
 * the other mode adaptive tokens. [FloatingDarkTheme] makes those read the dark scheme inside it, through the
 * public `KrailTheme` entry with a controller fixed to dark, so no component has its colours picked here.
 */
internal object FloatingDark {

    private const val PLACEHOLDER_ALPHA = 0.7f

    /** The fill of every floating card and button. Dark in both app modes. */
    val container: Color
        @Composable get() = KrailTheme.colors.stopLabelSurface

    /** What is drawn on [container]: text, icons and outlines. Light in both app modes. */
    val content: Color
        @Composable get() = KrailTheme.colors.onStopLabelSurface

    /**
     * A bar that is a row on the card's own surface: no fill of its own, light text and caret, and a
     * placeholder a step quieter than a typed stop. The same colours whether the bar has focus or not.
     */
    @Composable
    fun barColors(): TextFieldColors = TextFieldColors(
        containerColor = Color.Transparent,
        contentColor = content,
        placeholderColor = content.copy(alpha = PLACEHOLDER_ALPHA),
        cursorColor = content,
    )
}

/**
 * Everything inside reads the dark scheme whatever the app mode is. Held to the floating cards and
 * buttons only; nothing else on the screen is inside it.
 */
@Composable
internal fun FloatingDarkTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(currentMode = ThemeMode.DARK, setThemeMode = {}) }
    KrailTheme(themeController = controller, content = content)
}
