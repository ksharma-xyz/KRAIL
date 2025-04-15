package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.alerts.CollapsibleAlert
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

@Composable
fun IntroContentAlerts(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            var displayAlert by rememberSaveable { mutableStateOf(false) }

            AlertButton(
                dimensions = ButtonDefaults.smallButtonSize(),
                onClick = { displayAlert = !displayAlert },
            ) { Text(text = "2 Alerts") }

            AnimatedAlerts(displayAlert)
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "⚠",
            emojiColor = style.hexToComposeColor(),
            tagColor = style.hexToComposeColor(),
        )
    }
}


@Composable
fun AnimatedAlerts(displayAlert: Boolean) {
    AnimatedVisibility(
        visible = displayAlert,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = tween(300),
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { -20 },
                    animationSpec = tween(300),
                )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Running late. Please allow extra travel time.", message = "",
                ),
                index = 1,
                collapsed = false,
                onClick = {},
            )

            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Platforms may change, listen for announcements.", message = "",
                ),
                index = 2,
                collapsed = false,
                onClick = {},
            )
        }
    }
}