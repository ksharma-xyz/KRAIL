package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun IntroContentSaveTrips(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OriginDestination(
                trip = trip,
                timeLineColor = KrailTheme.colors.onSurface,
            )

            Divider()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var isTripSaved by rememberSaveable { mutableStateOf(false) }
            val rotation = remember { Animatable(0f) }
            val coroutineScope = rememberCoroutineScope()
            var autoRotationDir by rememberSaveable { mutableStateOf(1f) }
            var rotationJob by remember { mutableStateOf<Job?>(null) }

            fun startAutoRotation() {
                rotationJob?.cancel()
                rotationJob = coroutineScope.launch {
                    while (isActive) {
                        rotation.animateTo(
                            targetValue = rotation.value + 360f * autoRotationDir,
                            animationSpec = tween(durationMillis = 2000)
                        )
                        autoRotationDir *= -1f
                        delay(200) // Small pause between rotations
                    }
                }
            }

            LaunchedEffect(Unit) {
                startAutoRotation()
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = style.hexToComposeColor())
                    .clickable(
                        indication = null,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            rotationJob?.cancel()
                            coroutineScope.launch {
                                rotation.animateTo(
                                    targetValue = rotation.value + 360f,
                                    animationSpec = tween(durationMillis = 500)
                                )
                                startAutoRotation()
                            }
                            isTripSaved = !isTripSaved
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = if (isTripSaved) {
                        painterResource(Res.drawable.ic_star_filled)
                    } else {
                        painterResource(Res.drawable.ic_star)
                    },
                    contentDescription = if (isTripSaved) {
                        "Remove Saved Trip"
                    } else {
                        "Save Trip"
                    },
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(48.dp)
                        .graphicsLayer(rotationZ = rotation.value),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83C\uDF1F",
            tagColor = style.hexToComposeColor()
        )
    }
}

private val trip = Trip(
    fromStopName = "Wynyard Station",
    toStopName = "Central Station",
    fromStopId = "1",
    toStopId = "2"
)
