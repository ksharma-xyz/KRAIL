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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
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
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.discover.network.api.DiscoverCardButtonRowState
import xyz.ksharma.krail.discover.network.api.DiscoverCardModel
import xyz.ksharma.krail.discover.network.api.toButtonRowState
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun DiscoverCard(
    discoverCardModel: DiscoverCardModel = DiscoverCardModel(
        title = "The title of the Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://i0.wp.com/spacenews.com/wp-content/uploads/2025/01/Thuraya-4-scaled.jpg",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Cta(
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

        discoverCardModel.buttons?.let { buttonsList ->
            DiscoverCardButtonRow(buttonsList)
        }
    }
}

@Composable
private fun DiscoverCardButtonRow(buttonsList: List<DiscoverCardModel.Button>) {
    val state = buttonsList.toButtonRowState()
    if (state == null) {
        logError("Invalid button combination or no buttons provided: ${buttonsList.map { it::class.simpleName }}")
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val left = state.left) {
            is DiscoverCardButtonRowState.LeftButtonType.Cta -> {
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    onClick = {}
                ) {
                    Text(text = left.button.label)
                }
            }

            is DiscoverCardButtonRowState.LeftButtonType.Social -> {
                SocialConnectionBox()
            }

            is DiscoverCardButtonRowState.LeftButtonType.Feedback -> Row(
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                FeedbackCircleBox()
                FeedbackCircleBox()
            }

            null -> Unit
        }

        when (val right = state.right) {
            is DiscoverCardButtonRowState.RightButtonType.Share -> {

            }

            null -> Unit
        }
    }
}

@Composable
private fun SocialConnectionBox() {
    // Replace with your SocialConnectionBox implementation
    Box(
        modifier = Modifier.size(40.dp).background(Color.Blue, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("S", color = Color.White)
    }
}

@Composable
private fun FeedbackCircleBox() {
    Box(
        modifier = Modifier.size(40.dp).background(Color.Yellow, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("F", color = Color.Black)
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

private class DiscoverCardProvider : PreviewParameterProvider<DiscoverCardModel> {
    override val values: Sequence<DiscoverCardModel>
        get() = sequenceOf(
            DiscoverCardModel(
                title = "Discover Card Title",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = persistentListOf(),
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = persistentListOf(DiscoverCardModel.Button.Social)
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = persistentListOf(
                    DiscoverCardModel.Button.Share(
                        shareUrl = "https://example.com/share",
                    ),
                )
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = persistentListOf(
                    DiscoverCardModel.Button.Feedback(
                        label = "Feedback",
                        url = "https://example.com/feedback",
                    ),
                )
            ),
            DiscoverCardModel(
                title = "Social Card",
                description = "This is a sample description for the Discover Card. It can be used to display additional information.",
                imageUrl = "https://example.com/image.jpg",
                buttons = persistentListOf(
                    DiscoverCardModel.Button.Cta(
                        label = "Click Me",
                        url = "https://example.com/cta",
                    ),
                )
            ),
        )
}
