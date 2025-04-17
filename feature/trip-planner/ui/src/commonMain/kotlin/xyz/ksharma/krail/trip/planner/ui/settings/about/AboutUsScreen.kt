package xyz.ksharma.krail.trip.planner.ui.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun AboutUsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Our Story") },
            )
        }

        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp),
        ) {
            item {
                Text(
                    "KRAIL - Ride the rail without fail.",
                    style = KrailTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }

            item {
                Text(
                    "Thank you for downloading the KRAIL App. If the app has helped you, " +
                            "please review on App Store and share with friends. You can save trips " +
                            "and see real-time public transport information across Sydney, NSW. " +
                            "The real time trip data is provided by Transport for NSW. " +
                            "Best efforts are taken to ensure the " +
                            "accuracy of the data. However, no guarantees are made. " +
                            "Please refer to the Transport for NSW website (www.transportnsw.info) for more " +
                            "information.",
                    style = KrailTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }
    }
}
