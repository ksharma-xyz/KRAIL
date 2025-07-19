package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.discover.network.api.DiscoverCard
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun DiscoverCard(
    discoverCard: DiscoverCard,
    modifier: Modifier = Modifier,
    onClick: (DiscoverCard) -> Unit = {},
) {
    Column(
        modifier = modifier
            .size(height = 600.dp, width = 420.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Text(
            text = discoverCard.title,
            modifier = Modifier.padding(16.dp),
            maxLines = 2,
            style = KrailTheme.typography.titleMedium,
        )
        Text(
            text = discoverCard.title,
            modifier = Modifier.padding(16.dp),
            maxLines = 2,
            style = KrailTheme.typography.bodyMedium,
        )

       /* Row {
            discoverCard.buttons
        }*/
    }
}