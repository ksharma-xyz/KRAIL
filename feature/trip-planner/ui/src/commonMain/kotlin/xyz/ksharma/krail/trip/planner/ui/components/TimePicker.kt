package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KrailTimePicker(
    timePickerState: TimePickerState,
) {
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    val themeContentColor = getForegroundColor(themeColor)

    TimePicker(
        state = timePickerState,
        colors = TimePickerColors(
            containerColor = KrailTheme.colors.surface, // Set container color to surface
            clockDialColor = KrailTheme.colors.surface, // Set clock dial color to surface
            selectorColor = themeColor, // Set selector color to themeColor
            periodSelectorBorderColor = themeColor, // Set period selector border color to themeColor
            clockDialSelectedContentColor = themeContentColor, // Set selected clock dial content color to themeColor
            clockDialUnselectedContentColor = KrailTheme.colors.onSurface.copy(
                alpha = 0.6f
            ), // Set unselected clock dial content color to onSurface with reduced alpha
            periodSelectorSelectedContainerColor = themeColor, // Set selected period selector container color to themeColor
            periodSelectorUnselectedContainerColor = KrailTheme.colors.surface, // Set unselected period selector container color to surface
            periodSelectorSelectedContentColor = themeContentColor, // Set selected period selector content color to onSurface
            periodSelectorUnselectedContentColor = KrailTheme.colors.onSurface.copy(
                alpha = 0.6f
            ), // Set unselected period selector content color to onSurface with reduced alpha
            timeSelectorSelectedContainerColor = themeColor, // Set selected time selector container color to themeColor
            timeSelectorUnselectedContainerColor = KrailTheme.colors.surface, // Set unselected time selector container color to surface
            timeSelectorSelectedContentColor = themeContentColor, // Set selected time selector content color to onSurface
            timeSelectorUnselectedContentColor = KrailTheme.colors.onSurface.copy(
                alpha = 0.6f
            ) // Set unselected time selector content color to onSurface with reduced alpha
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
