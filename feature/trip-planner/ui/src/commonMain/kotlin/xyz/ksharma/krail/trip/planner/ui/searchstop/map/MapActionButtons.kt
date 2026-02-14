package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Options button for the map.
 * Camera movement automatically triggers stop loading, so no manual refresh needed.
 *
 * @param onOptionsClick Callback for options button click
 * @param modifier Modifier to be applied to the component
 */
@Composable
internal fun MapActionButtons(
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onOptionsClick,
        dimensions = ButtonDefaults.mediumButtonSize(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, bottom = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_filter),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ButtonDefaults.buttonColors().contentColor),
                modifier = Modifier.size(18.dp),
            )
            Text(text = "Options")
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewMapActionButtons() {
    PreviewTheme {
        MapActionButtons(
            onOptionsClick = {},
        )
    }
}

// endregion
