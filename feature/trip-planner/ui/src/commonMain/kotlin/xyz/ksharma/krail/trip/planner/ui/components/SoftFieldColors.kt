package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The colours of a soft pill search field, in the one place, for taj's `TextField` through its exposed `colors` parameter and
 * nothing else. The fill is a touch darker than the page in light mode and a touch lighter in dark mode, so the pill has a
 * visible edge on the page without being the harsh near black bar of `invertedColors()`:
 *
 * - [TextFieldColors.containerColor] is `pastDepartureRowSurface`, the soft grey the app already uses for a quiet row. It is
 *   about 1.16 to 1 against the page in light mode and 1.22 to 1 in dark mode, so it reads as a fill and not as a border.
 * - [TextFieldColors.contentColor] and [TextFieldColors.cursorColor] are `onSurface`, the page's own text colour.
 * - [TextFieldColors.placeholderColor] is `labelPlaceholder`, taj's own placeholder token, which is what taj's placeholder
 *   draws by default. `softLabel` is not used, because on this fill it is 3.9 to 1 in light mode, under the 4.5 text needs.
 *
 * On the fill the text is 18.0 to 1 in light mode and 13.2 in dark, and the placeholder 6.0 and 5.8, so both clear 4.5 to 1,
 * which `IntroStopFieldColorsTest` and `SoftFieldColorsTest` hold.
 */
@Composable
internal fun softFieldColors(): TextFieldColors = TextFieldColors(
    containerColor = KrailTheme.colors.pastDepartureRowSurface,
    contentColor = KrailTheme.colors.onSurface,
    placeholderColor = KrailTheme.colors.labelPlaceholder,
    cursorColor = KrailTheme.colors.onSurface,
)
