package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardButtonRowState
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.state.toButtonRowState
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.social.state.SocialType
import xyz.ksharma.krail.social.ui.SocialConnectionRow
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.discoverCardHeight
import xyz.ksharma.krail.taj.isLargeFontScale
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import app.krail.taj.resources.Res as TajRes
import coil3.request.CachePolicy
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun DiscoverCard(
    discoverModel: DiscoverState.DiscoverUiModel,
    modifier: Modifier = Modifier,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit = {},
    onPartnerSocialLinkClicked: (
        Button.Social.PartnerSocial.PartnerSocialLink,
        String,
        DiscoverCardType
    ) -> Unit = { _, _, _ -> },
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit = { _, _, _ -> },
    onFeedbackCta: (Boolean) -> Unit = {},
    onFeedbackThumb: (Boolean) -> Unit = {},
    onShareClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(discoverCardHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(color = themeBackgroundColor()),
    ) {
        BoxWithConstraints {
            val maxCardWidth = maxWidth
            val imageHeight = discoverCardHeight * if (isLargeFontScale()) 0.5f else 0.6f
            val context = LocalPlatformContext.current

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .crossfade(true)
                    .data(discoverModel.imageList.firstOrNull())
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .networkCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(maxCardWidth)
                    .height(imageHeight)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }

        Text(
            text = discoverModel.title,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            maxLines = 2,
            style = KrailTheme.typography.headlineSmall,
        )

        Text(
            text = discoverModel.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = if (isLargeFontScale()) 2 else 3,
            style = KrailTheme.typography.bodyMedium,
        )

        discoverModel.disclaimer?.let { disclaimer ->
            Text(
                text = disclaimer,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 1,
                style = KrailTheme.typography.labelSmall,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        discoverModel.buttons?.let { buttonsList ->
            DiscoverCardButtonRow(
                buttonsList = buttonsList,
                onAppSocialLinkClicked = onAppSocialLinkClicked,
                onPartnerSocialLinkClicked = { partnerSocialLink ->
                    onPartnerSocialLinkClicked(
                        partnerSocialLink, discoverModel.cardId, discoverModel.type
                    )
                },
                onCtaClicked = { url ->
                    onCtaClicked(url, discoverModel.cardId, discoverModel.type)
                },
                onFeedbackCta = onFeedbackCta,
                onFeedbackThumb = onFeedbackThumb,
                onShareClick = onShareClick,
            )
        }
    }
}

@Composable
private fun DiscoverCardButtonRow(
    buttonsList: List<Button>,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink) -> Unit,
    onCtaClicked: (String) -> Unit,
    onFeedbackThumb: (Boolean) -> Unit,
    onFeedbackCta: (Boolean) -> Unit,
    onShareClick: (String) -> Unit,
) {
    val state = buttonsList.toButtonRowState()
    if (state == null) {
        logError("Invalid button combination or no buttons provided: ${buttonsList.map { it::class.simpleName }}")
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left button (always left-aligned)
        when (val left = state.left) {
            is DiscoverCardButtonRowState.LeftButtonType.Cta -> {
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    onClick = {
                        onCtaClicked(left.button.url)
                    },
                ) {
                    Text(text = left.button.label)
                }
            }

            is DiscoverCardButtonRowState.LeftButtonType.Social -> {
                when (val socialButton = left.button) {
                    Button.Social.AppSocial -> {
                        SocialConnectionRow(
                            onClick = { krailSocialType ->
                                onAppSocialLinkClicked(krailSocialType)
                            },
                            socialLinks = KrailSocialType.entries,
                        )
                    }

                    is Button.Social.PartnerSocial -> {
                        SocialConnectionRow(
                            onClick = { partnerSocialType ->
                                onPartnerSocialLinkClicked(partnerSocialType)
                            },
                            socialPartnerName = socialButton.socialPartnerName,
                            partnerSocialLinks = socialButton.links,
                        )
                    }
                }
            }

            is DiscoverCardButtonRowState.LeftButtonType.Feedback -> {
                FeedbackButtonsRow(
                    onNegativeThumb = {
                        onFeedbackThumb(false)
                    },
                    onPositiveThumb = {
                        onFeedbackThumb(true)
                    },
                    onNegativeCta = {
                        onFeedbackCta(false)
                    },
                    onPositiveCta = {
                        onFeedbackCta(true)
                    },
                )
            }

            null -> Unit
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right button (always right-aligned)
        when (val rightButton = state.right) {
            is DiscoverCardButtonRowState.RightButtonType.Share -> {
                RoundIconButton(
                    onClick = { onShareClick(rightButton.button.shareUrl) },
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

            null -> Unit
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun DiscoverCardCtaPreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[0])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardNoButtonsPreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[1])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardSocialPreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[2])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardSharePreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[3])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardCtaSharePreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[4])
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverCardFeedbackPreview() {
    PreviewContent {
        DiscoverCard(discoverModel = previewDiscoverCardList[5])
    }
}

private class DiscoverCardProvider : PreviewParameterProvider<DiscoverState.DiscoverUiModel> {
    override val values: Sequence<DiscoverState.DiscoverUiModel>
        get() = previewDiscoverCardList.asSequence()
}

val previewDiscoverCardList = listOf(
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_1",
        title = "Cta only card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1749751234397-41ee8a7887c2"),
        type = DiscoverCardType.Travel,
        buttons = persistentListOf(
            Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        ),
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_2",
        title = "No Buttons Card Title",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1741851373856-67a519f0c014"),
        type = DiscoverCardType.Events,
        buttons = persistentListOf(),
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_3",
        title = "App Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        type = DiscoverCardType.Krail,
        buttons = persistentListOf(Button.Social.AppSocial)
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_4",
        title = "Partner Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        type = DiscoverCardType.Events,
        buttons = persistentListOf(
            Button.Social.PartnerSocial(
                socialPartnerName = "XYZ Place",
                links = persistentListOf(
                    Button.Social.PartnerSocial.PartnerSocialLink(
                        type = SocialType.Facebook,
                        url = "https://example.com"
                    ),
                )
            )
        )
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_4",
        title = "Share Only Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1751906599846-2e31345c8014"),
        type = DiscoverCardType.Kids,
        buttons = persistentListOf(
            Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_5",
        title = "Cta + Share Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1730005745671-dbbfddfe387e"),
        type = DiscoverCardType.Sports,
        buttons = persistentListOf(
            Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
            Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_6",
        title = "Feedback Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752832756659-4dd7c40f5ae7"),
        type = DiscoverCardType.Events,
        buttons = persistentListOf(
            Button.Feedback(
                label = "Feedback",
                url = "https://example.com/feedback",
            ),
        )
    ),
    DiscoverState.DiscoverUiModel(
        cardId = "cta_card_7",
        title = "Credits Disclaimer Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1735746693939-586a9d7558e1"),
        type = DiscoverCardType.Events,
        disclaimer = "Image credits: Unsplash",
        buttons = persistentListOf(
            Button.Cta(
                label = "Cta Button",
                url = "https://example.com/feedback",
            ),
        )
    ),
)

// endregion Previews
