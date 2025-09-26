package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.trip.planner.ui.components.OutlineRadioButton
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions

@Composable
fun JourneyTimeOptionsGroup(
    selectedOption: JourneyTimeOptions = JourneyTimeOptions.LEAVE,
    themeColor: Color,
    modifier: Modifier = Modifier,
    onOptionSelected: (JourneyTimeOptions) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        JourneyTimeOptions.entries.forEach { option ->
            OutlineRadioButton(
                text = option.text,
                selected = option == selectedOption,
                themeColor = themeColor,
                onClick = { onOptionSelected(option) },
            )
        }
    }
}
