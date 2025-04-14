package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun IntroContentSaveTrips(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        Text(
            text = "JUST\nONE TAP\nTHAT'S IT",
            style = KrailTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun IntroContentRealTime(modifier: Modifier = Modifier) {
    Text("FASTEST\nROUTES\nEVERYTIME")
}

@Composable
fun IntroContentAlerts(modifier: Modifier = Modifier) {
    Text("WE\nRESPECT\nYOUR TIME")
}

@Composable
fun IntroContentPlanTrip(modifier: Modifier = Modifier) {
    Text("WE\nCAN TELL\nTHE FUTURE")

}

@Composable
fun IntroContentInviteFriends(modifier: Modifier = Modifier) {
    Text("RIDING SOLO\nNAH THANKS")


    Text("LET'S\nKRAIL\nTOGETHER")
}

@Composable
fun IntroContentSelectMode(modifier: Modifier = Modifier) {
    Text("TRAIN, BUS\nOR BOTH\nYOUR CHOICE")
}


