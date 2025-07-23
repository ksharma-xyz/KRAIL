package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import xyz.ksharma.krail.taj.themeBackgroundColor

@Composable
fun DiscoverCard(
    discoverCardModel: DiscoverCardModel,
    modifier: Modifier = Modifier,
    onClick: (DiscoverCardModel) -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(550.dp) // todo - common var
            .clip(RoundedCornerShape(24.dp))
            .background(color = themeBackgroundColor()),
    ) {
        BoxWithConstraints {
            val maxCardWidth = maxWidth
            val isTablet = maxWidth > 600.dp
            val imageRatio = if (isTablet) 1.2f else 1f // Slightly wider on tablets

            AsyncImage(
                model = discoverCardModel.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(maxCardWidth)
                    .aspectRatio(imageRatio) // Maintains 1:1 ratio for square images
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }

        // todo - auto suze to fit one line
        // text can be max 5-6 words
        Text(
            text = discoverCardModel.title,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 2,
            style = KrailTheme.typography.titleLarge,
        )

        // max 18-20 words
        Text(
            text = discoverCardModel.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            maxLines = 2,

            style = KrailTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.weight(1f)) // Pushes buttons to bottom

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
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left button (always left-aligned)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeedbackCircleBox()
                FeedbackCircleBox()
            }

            null -> Box(modifier = Modifier) // Empty box to keep slot
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right button (always right-aligned)
        when (val right = state.right) {
            is DiscoverCardButtonRowState.RightButtonType.Share -> {
                // Replace with your Share button UI
                FeedbackCircleBox()
            }

            null -> Box(modifier = Modifier) // Empty box to keep slot
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
        get() = discoverCardList.asSequence()
}

val discoverCardList = listOf(
    DiscoverCardModel(
        title = "Cta only card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://images.unsplash.com/photo-1749751234397-41ee8a7887c2",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        ),
    ),
    DiscoverCardModel(
        title = "No Buttons Card Title 2",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://plus.unsplash.com/premium_photo-1752367225760-34f565f0720f",
        buttons = persistentListOf(),
    ),
    DiscoverCardModel(
        title = "Social Card 3",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b",
        buttons = persistentListOf(DiscoverCardModel.Button.Social)
    ),
    DiscoverCardModel(
        title = "Share Only Card 4",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://plus.unsplash.com/premium_photo-1751906599846-2e31345c8014",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverCardModel(
        title = "Cta + Share Card 8",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://images.unsplash.com/photo-1752939124510-e444139e6404",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
            DiscoverCardModel.Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverCardModel(
        title = "Feedback Card 5",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://plus.unsplash.com/premium_photo-1752832756659-4dd7c40f5ae7",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Feedback(
                label = "Feedback",
                url = "https://example.com/feedback",
            ),
        )
    ),
    DiscoverCardModel(
        title = "Cta Card 6",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageUrl = "https://images.unsplash.com/photo-1752350434901-e1754a4784fe?q=80&w=830",
        buttons = persistentListOf(
            DiscoverCardModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        )
    ),
)