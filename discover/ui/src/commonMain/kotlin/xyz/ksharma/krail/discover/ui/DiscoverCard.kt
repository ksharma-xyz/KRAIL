package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_android_share
import app.krail.taj.resources.ic_ios_share
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.getAppPlatformType
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardButtonRowState
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.state.toButtonRowState
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.social.state.SocialType
import xyz.ksharma.krail.social.ui.SocialConnectionRow
import xyz.ksharma.krail.taj.brighten
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.rememberCardHeight
import xyz.ksharma.krail.taj.darken
import xyz.ksharma.krail.taj.getImageHeightRatio
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import app.krail.taj.resources.Res as TajRes

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
    onShareClick: () -> Unit = {},
) {
    val discoverCardHeight = rememberCardHeight()

    // Create a blended background that combines theme color with surface
    val blendedBackground = createAdaptiveBackground()

    Column(
        modifier = modifier
            .height(discoverCardHeight)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        KrailTheme.colors.surface,           // Top: pure surface
                        blendedBackground.copy(alpha = 0.3f), // Middle: subtle blend
                        blendedBackground.copy(alpha = 0.6f)  // Bottom: stronger blend
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = themeBackgroundColor(),
                shape = RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp)),
    ) {
        BoxWithConstraints {
            val maxCardWidth = maxWidth
            val imageHeight = discoverCardHeight * getImageHeightRatio()
            val context = LocalPlatformContext.current

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(discoverModel.imageList.firstOrNull())
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .networkCachePolicy(CachePolicy.READ_ONLY)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(blendedBackground),
                modifier = Modifier
                    .width(maxCardWidth)
                    .height(imageHeight)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            )
        }

        Text(
            text = discoverModel.title,
            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp),
            maxLines = 2,
            style = KrailTheme.typography.titleLarge,
        )

        Text(
            text = discoverModel.description,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            maxLines = if (discoverModel.disclaimer != null) 2 else 3,
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.secondaryLabel,
        )

        discoverModel.disclaimer?.let { disclaimer ->
            Text(
                text = disclaimer,
                modifier = Modifier.padding(horizontal = 12.dp),
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
                onShareClick = onShareClick,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun createAdaptiveBackground(): Color {
    val themeColor = themeColor()
    val surfaceColor = KrailTheme.colors.surface

    return if (isSystemInDarkTheme()) {
        // Dark mode: blend theme color with darkened surface (towards black)
        blendColors(
            foreground = themeColor.copy(alpha = 0.1f), // Reduced alpha for subtlety
            background = surfaceColor.darken(0.15f)     // More darkening towards black
        )
    } else {
        // Light mode: blend theme color with brightened surface (towards white)
        blendColors(
            foreground = themeColor.copy(alpha = 0.1f), // Reduced alpha for subtlety
            background = surfaceColor.brighten(0.15f)    // More brightening towards white
        )
    }
}

/**
 * Blends two colors based on the alpha of the foreground color.
 */
private fun blendColors(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha
    return Color(
        red = foreground.red * alpha + background.red * (1 - alpha),
        green = foreground.green * alpha + background.green * (1 - alpha),
        blue = foreground.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}

@Composable
fun DiscoverCardButtonRow(
    buttonsList: List<Button>,
    modifier: Modifier = Modifier,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink) -> Unit,
    onCtaClicked: (String) -> Unit,
    onShareClick: () -> Unit,
) {
    val state = buttonsList.toButtonRowState()
    if (state == null) {
        logError(
            "Invalid button combination or no buttons provided: " +
                    "${buttonsList.map { it::class.simpleName }}"
        )
        return
    } else {
        log("Buttons state: left=${state.left}, right=${state.right}")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left button (always left-aligned)
        when (val left = state.left) {
            is DiscoverCardButtonRowState.LeftButtonType.Cta -> {
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    colors = ButtonDefaults.monochromeButtonColors(),
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

            null -> Unit
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right button (always right-aligned)
        when (val rightButton = state.right) {
            is DiscoverCardButtonRowState.RightButtonType.Share -> {
                log("Right button is Share")

                RoundIconButton(
                    onClick = onShareClick,
                    color = Color.Transparent,
                ) {
                    Image(
                        painter = if (getAppPlatformType() == DevicePlatformType.IOS) {
                            painterResource(TajRes.drawable.ic_ios_share)
                        } else {
                            painterResource(TajRes.drawable.ic_android_share)
                        },
                        contentDescription = "Share",
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            null -> {
                log("No right button to display")
            }
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
        type = DiscoverCardType.Food,
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
        type = DiscoverCardType.Travel,
        buttons = persistentListOf(Button.Share)
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
            Button.Share,
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
