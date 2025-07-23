package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.discover.ui.DiscoverCard
import xyz.ksharma.krail.discover.ui.discoverCardList
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.trip.planner.ui.components.DiscoverCardVerticalPager
import xyz.ksharma.krail.trip.planner.ui.state.discover.DiscoverState

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column {

            // Add the pager here
            DiscoverCardVerticalPager(
                items = discoverCardList,
                modifier = Modifier.fillMaxSize(),
                content = { cardModel ->
                    DiscoverCard(discoverCardModel = cardModel)
                }
            )
        }

        TitleBar(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.TopCenter),
            onNavActionClick = onBackClick,
            title = { },
        )

        /*Text(
            text = "What's On, Sydney!",
            style = KrailTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .background(color = Color.Transparent),
        )*/
    }
}
