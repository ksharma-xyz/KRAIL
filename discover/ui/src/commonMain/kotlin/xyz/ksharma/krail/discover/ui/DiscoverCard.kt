package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .aspectRatio(1f)
                .background(color = Color.Red)
        ) {}

        Text(
            text = discoverCard.title,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 2,
            style = KrailTheme.typography.displayMedium,
        )
        Text(
            text = discoverCard.title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 2,
            style = KrailTheme.typography.bodyLarge,
        )

        DiscoverCardButtonRow(discoverCard.buttons)
    }
}

@Composable
private fun DiscoverCardButtonRow(cardList: List<DiscoverCard.Button>?) {
    if (cardList.isNullOrEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

    }
}

@Preview
@Composable
private fun DiscoverCardPreview(
//    @PreviewParameter(DiscoverCardProvider::class) discoverCard: DiscoverCard,
) {
    KrailTheme {
        DiscoverCardProvider().values.forEach { discoverCard ->
            Column(modifier = Modifier.background(color = KrailTheme.colors.surface)) {
                DiscoverCard(discoverCard = discoverCard)
            }
        }
    }
}

class DiscoverCardProvider : PreviewParameterProvider<DiscoverCard> {
    override val values: Sequence<DiscoverCard>
        get() = sequenceOf(
            DiscoverCard(
                title = "Discover Card Title",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = emptyList(),
            ),
            DiscoverCard(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCard.Button(
                        type = DiscoverCard.ButtonType.SOCIAL,
                    ),
                )
            ),
            DiscoverCard(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCard.Button(
                        type = DiscoverCard.ButtonType.SHARE,
                        label = "Read More",
                        url = "https://example.com/readmore",
                        shareUrl = "https://example.com/share",
                    ),
                )
            ),
            DiscoverCard(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCard.Button(
                        type = DiscoverCard.ButtonType.FEEDBACK,
                        label = "Feedback",
                        url = "https://example.com/feedback",
                    ),
                )
            ),
            DiscoverCard(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCard.Button(
                        type = DiscoverCard.ButtonType.CTA,
                        label = "Click Me",
                        url = "https://example.com/cta",
                    ),
                )
            ),
        )
}
