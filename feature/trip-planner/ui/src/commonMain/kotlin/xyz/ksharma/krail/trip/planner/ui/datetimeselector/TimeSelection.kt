package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeInkColorOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelection(
    timePickerState: TimePickerState,
    modifier: Modifier = Modifier,
    displayTitle: Boolean = true,
) {
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    val themeContentColor = getForegroundColor(themeColor)

    // The dial is filled with the theme colour at 45% over the sheet, and the hand and the
    // selected-hour bubble are drawn on top of it. Drawn in the raw theme colour that is purple
    // on purple: Purple Drip measured 1.87:1 against its own dial, and every other theme failed
    // the same pairing. This is the one place ink lands on something that is not an app surface,
    // so it is adapted against the dial rather than against the scheme's hardest ground.
    val dialColor = themeBackgroundColor()
    // themeBackgroundColor is translucent, and contrastRatio reads luminance(), which ignores
    // alpha. Measuring against it directly reads the dial as far lighter than it renders and
    // over-adapts the hand to near-white. Composite it over the sheet it is drawn on first.
    val dialAsRendered = dialColor.compositeOver(KrailTheme.colors.bottomSheetBackground)
    val selectorInk = themeInkColorOn(
        background = dialAsRendered,
        minContrast = UI_COMPONENT_CONTRAST_AA,
    )

    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.padding(top = dim.spacingL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        val density = LocalDensity.current
        if (displayTitle) {
            Text(
                text = "Select Time",
                style = KrailTheme.typography.title,
                color = KrailTheme.colors.onSurface,
            )
        }

        CompositionLocalProvider(
            LocalDensity provides Density(
                (density.density - 0.6f).coerceIn(1.5f, 3f),
                fontScale = density.fontScale,
            ),
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerColors(
                    containerColor = dialColor,
                    clockDialColor = dialColor,
                    selectorColor = selectorInk,
                    periodSelectorBorderColor = selectorInk,
                    clockDialSelectedContentColor = themeContentColor,
                    clockDialUnselectedContentColor = KrailTheme.colors.onSurface.copy(alpha = 0.8f),
                    periodSelectorSelectedContainerColor = themeColor,
                    periodSelectorUnselectedContainerColor = KrailTheme.colors.surface,
                    periodSelectorSelectedContentColor = themeContentColor,
                    periodSelectorUnselectedContentColor = KrailTheme.colors.onSurface.copy(alpha = 0.6f),
                    timeSelectorSelectedContainerColor = themeColor,
                    timeSelectorUnselectedContainerColor = KrailTheme.colors.surface,
                    timeSelectorSelectedContentColor = themeContentColor,
                    timeSelectorUnselectedContentColor = KrailTheme.colors.onSurface.copy(alpha = 0.8f),
                ),
                layoutType = TimePickerLayoutType.Vertical,
                modifier = Modifier.fillMaxWidth().padding(top = dim.spacingL),
            )
        }
    }
}
