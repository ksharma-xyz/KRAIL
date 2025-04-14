package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun IntroContentSaveTrips(tagline: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun IntroContentRealTime(tagline: String,modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun IntroContentAlerts(tagline: String,modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun IntroContentPlanTrip(tagline: String,modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun IntroContentInviteFriends(tagline: String,modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("RIDING SOLO\nNAH THANKS")

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
        )
    }
}

@Composable
fun IntroContentSelectMode(tagline: String,modifier: Modifier = Modifier) {
    Column(modifier = modifier) {

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
        )
    }
}
