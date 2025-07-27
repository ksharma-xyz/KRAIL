package xyz.ksharma.krail.discover.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.getAppPlatformType
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.discover.network.api.DiscoverCardButtonRowState
import xyz.ksharma.krail.discover.network.api.DiscoverModel
import xyz.ksharma.krail.discover.network.api.previewDiscoverCardList
import xyz.ksharma.krail.discover.network.api.toButtonRowState
import xyz.ksharma.krail.social.network.api.model.KrailSocialType
import xyz.ksharma.krail.social.ui.SocialConnectionRow
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.discoverCardHeight
import xyz.ksharma.krail.taj.isLargeFontScale
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import app.krail.taj.resources.Res as TajRes

@Composable
fun DiscoverCard(
    discoverModel: DiscoverModel,
    modifier: Modifier = Modifier,
    onClick: (DiscoverModel) -> Unit = {},
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

            AsyncImage(
                model = discoverModel.imageList.firstOrNull(),
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
            maxLines = if(isLargeFontScale()) 2 else 3,
            style = if (isLargeFontScale()) KrailTheme.typography.titleMedium
            else KrailTheme.typography.headlineSmall,
        )

        Text(
            text = discoverModel.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = if(isLargeFontScale()) 2 else 3,
            style = if (isLargeFontScale()) KrailTheme.typography.bodySmall
            else KrailTheme.typography.bodyMedium,
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
            DiscoverCardButtonRow(buttonsList)
        }
    }
}

@Composable
private fun DiscoverCardButtonRow(buttonsList: List<DiscoverModel.Button>) {
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
                    dimensions = ButtonDefaults.mediumButtonSize(), onClick = {}) {
                    Text(text = left.button.label)
                }
            }

            is DiscoverCardButtonRowState.LeftButtonType.Social -> {
                when (val socialButton = left.button) {
                    DiscoverModel.Button.Social.AppSocial -> {
                        SocialConnectionRow(
                            onClick = {},
                            socialLinks = KrailSocialType.entries,
                        )
                    }

                    is DiscoverModel.Button.Social.PartnerSocial -> {
                        SocialConnectionRow(
                            onClick = {},
                            socialPartnerName = socialButton.socialPartnerName,
                            socialLinks = socialButton.links.map { it.type })
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

            null -> Unit
        }
    }
}

@Composable
private fun FeedbackCircleBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(40.dp).clip(shape = CircleShape).klickable(indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.title) {
            content()
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

private class DiscoverCardProvider : PreviewParameterProvider<DiscoverModel> {
    override val values: Sequence<DiscoverModel>
        get() = previewDiscoverCardList.asSequence()
}

// endregion Previews
