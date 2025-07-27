package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_android_share
import app.krail.taj.resources.ic_ios_share
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.getAppPlatformType
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.social.SocialConnectionRow
import xyz.ksharma.krail.core.social.model.KrailSocialType
import xyz.ksharma.krail.core.social.model.SocialType
import xyz.ksharma.krail.discover.network.api.DiscoverCardButtonRowState
import xyz.ksharma.krail.discover.network.api.DiscoverCardModel
import xyz.ksharma.krail.discover.network.api.DiscoverCardModel.Button.Social.PartnerSocial.PartnerSocialType
import xyz.ksharma.krail.discover.network.api.toButtonRowState
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.discoverCardHeight
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import app.krail.taj.resources.Res as TajRes

@Composable
fun DiscoverCard(
    discoverCardModel: DiscoverCardModel, // todo - map to ui model defined in ui module
    modifier: Modifier = Modifier,
    onClick: (DiscoverCardModel) -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(discoverCardHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(color = themeBackgroundColor()),
    ) {
        BoxWithConstraints {
            val maxCardWidth = maxWidth
            val imageHeight = discoverCardHeight * 0.6f

            AsyncImage(
                model = discoverCardModel.imageList.firstOrNull(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(maxCardWidth)
                    .height(imageHeight)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }

        // todo - auto suze to fit one line
        // text can be max 5-6 words
        Text(
            text = discoverCardModel.title,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            maxLines = 2, // for large font size 2, // for small font size 3
            style = KrailTheme.typography.headlineSmall,
        )

        // max 18-20 words
        Text(
            text = discoverCardModel.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 2, // for large font size 2, // for small font size 3
            style = KrailTheme.typography.bodyMedium,
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
                when (left.button) {
                    DiscoverCardModel.Button.Social.AppSocial -> {
                        SocialConnectionRow(onClick = {})
                    }

                    is DiscoverCardModel.Button.Social.PartnerSocial -> {
                        // TODO - Handle partner social links
                    }
                }
            }

            is DiscoverCardButtonRowState.LeftButtonType.Feedback -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    FeedbackCircleBox {
                        Text("👍")
                    }

                    FeedbackCircleBox {
                        Text("👎")
                    }
                }
            }

            null -> Unit
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right button (always right-aligned)
        when (state.right) {
            is DiscoverCardButtonRowState.RightButtonType.Share -> {
                RoundIconButton(
                    onClick = {},
                    color = Color.Transparent,
                ) {
                    Image(
                        painter = if (getAppPlatformType() == DevicePlatformType.IOS) {
                            painterResource(TajRes.drawable.ic_ios_share)
                        } else {
                            painterResource(TajRes.drawable.ic_android_share)
                        },
                        contentDescription = "Invite Friends",
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            null -> Box(modifier = Modifier) // Empty box to keep slot
        }
    }
}

@Composable
private fun FeedbackCircleBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(40.dp)
            .clip(shape = CircleShape)
            .klickable(indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.title) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardCtaPreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[0])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardNoButtonsPreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[1])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardSocialPreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[2])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardSharePreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[3])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardCtaSharePreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[4])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardFeedbackPreview() {
    PreviewContent {
        DiscoverCard(discoverCardModel = previewDiscoverCardList[5])
    }
}

private class DiscoverCardProvider : PreviewParameterProvider<DiscoverCardModel> {
    override val values: Sequence<DiscoverCardModel>
        get() = previewDiscoverCardList.asSequence()
}

val previewDiscoverCardList = listOf(
    DiscoverCardModel(
        title = "Cta only card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://images.unsplash.com/photo-1749751234397-41ee8a7887c2"),
        buttons = persistentListOf(
            DiscoverCardModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        ),
    ),
    DiscoverCardModel(
        title = "No Buttons Card Title",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://plus.unsplash.com/premium_photo-1752367225760-34f565f0720f"),
        buttons = persistentListOf(),
    ),
    DiscoverCardModel(
        title = "App Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        buttons = persistentListOf(DiscoverCardModel.Button.Social.AppSocial)
    ),
    DiscoverCardModel(
        title = "Partner Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        buttons = persistentListOf(
            DiscoverCardModel.Button.Social.PartnerSocial(
                links = listOf(
                    PartnerSocialType(type = SocialType.Facebook, url = "https://facebook.com"),
                )
            )
        )
    ),
    DiscoverCardModel(
        title = "Share Only Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://plus.unsplash.com/premium_photo-1751906599846-2e31345c8014"),
        buttons = persistentListOf(
            DiscoverCardModel.Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverCardModel(
        title = "Cta + Share Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://images.unsplash.com/photo-1752939124510-e444139e6404"),
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
        title = "Feedback Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = listOf("https://plus.unsplash.com/premium_photo-1752832756659-4dd7c40f5ae7"),
        buttons = persistentListOf(
            DiscoverCardModel.Button.Feedback(
                label = "Feedback",
                url = "https://example.com/feedback",
            ),
        )
    ),
)
