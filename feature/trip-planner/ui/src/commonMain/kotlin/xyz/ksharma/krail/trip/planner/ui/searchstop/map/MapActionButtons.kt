package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
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
 * Action buttons for the map (Options and Refresh).
 * Uses FlowRow to automatically wrap to column layout when:
 * - Font scale is large (> 1.4f)
 * - Screen width is small
 * - Buttons don't fit in allocated width
 *
 * @param maxWidth Maximum width constraint (70% of screen, reserving 30%)
 * @param isRefreshing Whether the refresh button is in loading state
 * @param onOptionsClick Callback for options button click
 * @param onRefreshClick Callback for refresh button click
 * @param modifier Modifier to be applied to the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapActionButtons(
    maxWidth: androidx.compose.ui.unit.Dp,
    isRefreshing: Boolean,
    onOptionsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .widthIn(max = maxWidth), // Reserve 30% of screen width
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Options button
        Button(
            onClick = onOptionsClick,
            dimensions = ButtonDefaults.mediumButtonSize(),
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

        // Refresh button
        Button(
            onClick = onRefreshClick,
            enabled = !isRefreshing,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ButtonDefaults.buttonColors().contentColor,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.ic_filter),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ButtonDefaults.buttonColors().contentColor),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(text = "Refresh")
            }
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewMapActionButtons() {
    PreviewTheme {
        MapActionButtons(
            maxWidth = 280.dp,
            isRefreshing = false,
            onOptionsClick = {},
            onRefreshClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewMapActionButtonsLoading() {
    PreviewTheme {
        MapActionButtons(
            maxWidth = 280.dp,
            isRefreshing = true,
            onOptionsClick = {},
            onRefreshClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewMapActionButtonsNarrow() {
    PreviewTheme {
        MapActionButtons(
            maxWidth = 180.dp, // Narrow width to show wrapping
            isRefreshing = false,
            onOptionsClick = {},
            onRefreshClick = {},
        )
    }
}

// endregion
