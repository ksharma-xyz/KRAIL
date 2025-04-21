package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
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
                onClick = {
                    displayAlert = !displayAlert
                    onInteraction()
                },
            ) { Text(text = "2 Alerts") }

            AnimatedAlertsOneByOne(displayAlert)
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "⚠",
            emojiColor = style.hexToComposeColor(),
            tagColor = style.hexToComposeColor(),
        )
    }
}


@Composable
fun AnimatedAlertsOneByOne(
    displayAlert: Boolean,
    delayMillis: Long = 300L // delay before starting second animation
) {
    var showFirst by rememberSaveable { mutableStateOf(false) }
    var showSecond by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(displayAlert) {
        if (displayAlert) {
            showFirst = true
            delay(delayMillis)
            showSecond = true
        } else {
            // hide second alert first then delay and hide the first alert
            showSecond = false
            delay(delayMillis)
            showFirst = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(
            visible = showFirst,
            enter = scaleIn(
                initialScale = 0f,
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = scaleOut(
                targetScale = 0f,
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Running late. Please allow extra travel time.",
                    message = ""
                ),
                index = 1,
                collapsed = false,
                onClick = {}
            )
        }
        AnimatedVisibility(
            visible = showSecond,
            enter = scaleIn(
                initialScale = 0f,
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = scaleOut(
                targetScale = 0f,
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Platforms may change, listen for announcements.",
                    message = ""
                ),
                index = 2,
                collapsed = false,
                onClick = {}
            )
        }
    }
}
