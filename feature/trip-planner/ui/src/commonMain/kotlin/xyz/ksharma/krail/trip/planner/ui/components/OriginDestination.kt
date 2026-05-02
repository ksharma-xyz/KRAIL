package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

private val ORIGIN_CIRCLE_SIZE = 10.dp
private val STOP_ROW_VERTICAL_PADDING = 12.dp

@Composable
internal fun OriginDestination(
    origin: StopDisplay,
    destination: StopDisplay,
    timeLineColor: Color,
    modifier: Modifier = Modifier,
    onOriginClick: ((StopDisplay) -> Unit)? = null,
    onDestinationClick: ((StopDisplay) -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    val cardShape = CardShape

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(color = themeBackgroundColor(), shape = cardShape),
    ) {
        StopRow(
            display = origin,
            isOrigin = true,
            timeLineColor = timeLineColor,
            onClick = onOriginClick,
        )
        Divider(modifier = Modifier.padding(start = dim.spacingXL + dim.iconSmall + dim.spacingM, end = dim.spacingL))
        StopRow(
            display = destination,
            isOrigin = false,
            timeLineColor = timeLineColor,
            onClick = onDestinationClick,
        )
    }
}

@Composable
private fun StopRow(
    display: StopDisplay,
    isOrigin: Boolean,
    timeLineColor: Color,
    onClick: ((StopDisplay) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val clickModifier = onClick?.let { handler ->
        Modifier.klickable { handler(display) }
    } ?: Modifier
    val displayText = if (display.label != null) {
        "${display.label} (${display.name})"
    } else {
        display.name
    }
    val labelIcon = display.label?.let { stopLabelIcon(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = dim.spacingXL, vertical = STOP_ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Box(
            modifier = Modifier.size(dim.iconSmall),
            contentAlignment = Alignment.Center,
        ) {
            when {
                labelIcon != null -> Image(
                    painter = painterResource(labelIcon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    modifier = Modifier.fillMaxSize(),
                )
                isOrigin -> Box(
                    modifier = Modifier
                        .size(ORIGIN_CIRCLE_SIZE)
                        .background(timeLineColor, CircleShape),
                )
                else -> Image(
                    painter = painterResource(Res.drawable.ic_location_on),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(timeLineColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AnimatedContent(
            targetState = displayText,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(200)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(500, easing = EaseOutBounce),
                        )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(500),
                        )
                    )
            },
            contentAlignment = Alignment.CenterStart,
            label = if (isOrigin) "originStopName" else "destinationStopName",
        ) { targetText ->
            Text(
                text = targetText,
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
            )
        }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewOriginDestination_Unlabelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        OriginDestination(
            origin = StopDisplay(stopId = "1", name = "Central Station"),
            destination = StopDisplay(stopId = "2", name = "Town Hall Station"),
            timeLineColor = themeColor(),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewOriginDestination_BothLabelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        OriginDestination(
            origin = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
            destination = StopDisplay(stopId = "2", name = "Town Hall Station", label = "Work"),
            timeLineColor = themeColor(),
        )
    }
}

@ScreenshotTest
@Preview(name = "Origin labelled only — Ferry")
@Composable
private fun PreviewOriginDestination_OriginLabelledOnly() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        OriginDestination(
            origin = StopDisplay(stopId = "1", name = "Manly Wharf", label = "Home"),
            destination = StopDisplay(stopId = "2", name = "Circular Quay Wharf"),
            timeLineColor = themeColor(),
        )
    }
}

// endregion
