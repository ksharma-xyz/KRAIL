package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

val cardWidth = 320.dp
val cardHeight = 400.dp
val imageHeight = (cardWidth * 9f / 16f)

@Composable
fun DiscoverCardTablet(
    discoverModel: DiscoverState.DiscoverUiModel,
    modifier: Modifier = Modifier,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit = {},
    onPartnerSocialLinkClicked: (
        Button.Social.PartnerSocial.PartnerSocialLink,
        String,
        DiscoverCardType
    ) -> Unit = { _, _, _ -> },
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit = { _, _, _ -> },
    onShareClick: (String) -> Unit = {},
) {
    val blendedBackground = createAdaptiveBackground()

    Column(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        KrailTheme.colors.surface,
                        blendedBackground.copy(alpha = 0.3f),
                        blendedBackground.copy(alpha = 0.6f)
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
                .fillMaxWidth()
                .height(imageHeight)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = discoverModel.title,
                modifier = Modifier.padding(top = 12.dp),
                maxLines = 2,
                style = KrailTheme.typography.titleMedium,
            )

            Text(
                text = discoverModel.description,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = if (discoverModel.disclaimer != null) 2 else 3,
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.secondaryLabel,
            )

            discoverModel.disclaimer?.let { disclaimer ->
                Text(
                    text = disclaimer,
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
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}