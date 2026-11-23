package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
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

    /** What is drawn on [container] a step quieter than [content]: a time, a placeholder. */
    val softContent: Color
        @Composable get() = content.copy(alpha = PLACEHOLDER_ALPHA)

    /**
     * taj's filled [xyz.ksharma.krail.taj.components.Button] in the one dark: the fixed container with the fixed
     * light content, passed through the button's own `colors` parameter and nothing else.
     */
    @Composable
    fun buttonColors(): ButtonColors = ButtonDefaults.buttonColors(
        customContainerColor = container,
        customContentColor = content,
    )

    /**
     * taj's [xyz.ksharma.krail.taj.components.OutlinedButton] on a fixed dark card: no fill, the fixed light
     * content for the text and the outline.
     */
    @Composable
    fun outlinedButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
        customContainerColor = Color.Transparent,
        customContentColor = content,
    )

    /**
     * A field that is its own pill on the map: the fixed dark fill, light text and caret, and a placeholder a
     * step quieter than a typed word. The same colours whether the field has focus or not.
     */
    @Composable
    fun fieldColors(): TextFieldColors = TextFieldColors(
        containerColor = container,
        contentColor = content,
        placeholderColor = softContent,
        cursorColor = content,
    )
}

/**
 * The fixed light for the text and icons of a card that floats on the map: taj's own text and icon colour
 * locals, so every taj text without a colour of its own reads [FloatingDark.content] in both app modes.
 */
@Composable
internal fun FloatingContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextColor provides FloatingDark.content,
        LocalContentColor provides FloatingDark.content,
        content = content,
    )
}

/**
 * Everything inside reads the dark scheme whatever the app mode is. Held to the places that reuse a
 * component which reads mode adaptive tokens inside and takes no colours (the departure board's rows and the
 * label pills); nothing else on the screen is inside it.
 */
@Composable
internal fun FloatingDarkTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(currentMode = ThemeMode.DARK, setThemeMode = {}) }
    KrailTheme(themeController = controller, content = content)
}
