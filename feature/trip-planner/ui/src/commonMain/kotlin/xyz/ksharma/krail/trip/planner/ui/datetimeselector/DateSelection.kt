package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_chevron_left
import krail.feature.trip_planner.ui.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.IconButton

@Composable
fun DateSelection(
    themeColor: Color,
    date: String,
    modifier: Modifier = Modifier,
    onNextClicked: () -> Unit = {},
    onPreviousClicked: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Select Date",
            style = KrailTheme.typography.title,
            color = KrailTheme.colors.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                painter = painterResource(Res.drawable.ic_chevron_left),
                color = themeColor,
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = onPreviousClicked,
            )
            Text(
                text = date,
                style = KrailTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(
                painter = painterResource(Res.drawable.ic_chevron_right),
                color = themeColor,
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = onNextClicked,
            )
        }
    }
}
