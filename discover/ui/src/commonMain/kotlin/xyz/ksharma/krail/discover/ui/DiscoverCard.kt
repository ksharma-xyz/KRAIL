package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import xyz.ksharma.krail.discover.network.api.DiscoverCardModel
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun DiscoverCard(
    discoverCardModel: DiscoverCardModel = DiscoverCardModel(
        title = "The title of the Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://i0.wp.com/spacenews.com/wp-content/uploads/2025/01/Thuraya-4-scaled.jpg",
        buttons = listOf(
            DiscoverCardModel.Button(
                type = DiscoverCardModel.ButtonType.CTA,
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        )
    ),
    modifier: Modifier = Modifier,
    onClick: (DiscoverCardModel) -> Unit = {},
) {
    Column(
        modifier = modifier
            .size(height = 480.dp, width = 320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color = Color.LightGray),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = discoverCardModel.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier.aspectRatio(1f),
            )
        }

        Text(
            text = discoverCardModel.title,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 2,
            style = KrailTheme.typography.displayMedium,
        )
        Text(
            text = discoverCardModel.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 2,
            style = KrailTheme.typography.bodyLarge,
        )

        DiscoverCardButtonRow(discoverCardModel.buttons)
    }
}

@Composable
private fun DiscoverCardButtonRow(cardList: List<DiscoverCardModel.Button>?) {
    if (cardList.isNullOrEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
private fun DiscoverCardPreview(
//    @PreviewParameter(DiscoverCardProvider::class) discoverCard: DiscoverCard,
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Blue.toArgb())
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        KrailTheme {
            DiscoverCardProvider().values.forEach { discoverCard ->
                Column(modifier = Modifier.background(color = KrailTheme.colors.surface)) {
                    DiscoverCard(discoverCardModel = discoverCard)
                }
            }
        }
    }
}

class DiscoverCardProvider : PreviewParameterProvider<DiscoverCardModel> {
    override val values: Sequence<DiscoverCardModel>
        get() = sequenceOf(
            DiscoverCardModel(
                title = "Discover Card Title",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = emptyList(),
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCardModel.Button(
                        type = DiscoverCardModel.ButtonType.SOCIAL,
                    ),
                )
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCardModel.Button(
                        type = DiscoverCardModel.ButtonType.SHARE,
                        label = "Read More",
                        url = "https://example.com/readmore",
                        shareUrl = "https://example.com/share",
                    ),
                )
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCardModel.Button(
                        type = DiscoverCardModel.ButtonType.FEEDBACK,
                        label = "Feedback",
                        url = "https://example.com/feedback",
                    ),
                )
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = listOf(
                    DiscoverCardModel.Button(
                        type = DiscoverCardModel.ButtonType.CTA,
                        label = "Click Me",
                        url = "https://example.com/cta",
                    ),
                )
            ),
        )
}
