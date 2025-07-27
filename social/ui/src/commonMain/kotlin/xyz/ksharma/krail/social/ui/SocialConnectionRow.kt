package xyz.ksharma.krail.social.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.social.ui.generated.resources.Res
import krail.social.ui.generated.resources.ic_facebook
import krail.social.ui.generated.resources.ic_instagram
import krail.social.ui.generated.resources.ic_linkedin
import krail.social.ui.generated.resources.ic_reddit
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.social.network.api.model.KrailSocialType
import xyz.ksharma.krail.social.network.api.model.SocialType
import xyz.ksharma.krail.social.network.api.model.SocialType.Facebook
import xyz.ksharma.krail.social.network.api.model.SocialType.Instagram
import xyz.ksharma.krail.social.network.api.model.SocialType.LinkedIn
import xyz.ksharma.krail.social.network.api.model.SocialType.Reddit
import xyz.ksharma.krail.taj.components.SocialConnectionIcon
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor

@Composable
fun SocialConnectionRow(
    socialLinks: List<KrailSocialType>,
    modifier: Modifier = Modifier,
    onClick: (KrailSocialType) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        socialLinks.forEach { socialType ->
            SocialConnectionIcon(
                onClick = { onClick(socialType) },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(resource = socialType.resource()),
                    contentDescription = "${socialType.socialType} Page for KRAIL App",
                    colorFilter = ColorFilter.tint(color = getForegroundColor(backgroundColor = themeBackgroundColor())),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun SocialConnectionRow(
    socialPartnerName: String,
    socialLinks: List<SocialType>,
    modifier: Modifier = Modifier,
    onClick: (SocialType) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        socialLinks.forEach { socialType ->
            SocialConnectionIcon(
                onClick = { onClick(socialType) },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(resource = socialType.resource()),
                    contentDescription = "${socialType.name} Page for $socialPartnerName",
                    colorFilter = ColorFilter.tint(color = getForegroundColor(backgroundColor = themeBackgroundColor())),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SocialConnectionRowPreview() {
    KrailTheme {
        SocialConnectionRow(
            socialLinks = KrailSocialType.entries,
            onClick = { /* Handle click */ },
        )
    }
}

fun KrailSocialType.resource(): DrawableResource = when (this) {
    KrailSocialType.LinkedIn -> Res.drawable.ic_linkedin
    KrailSocialType.Reddit -> Res.drawable.ic_reddit
    KrailSocialType.Instagram -> Res.drawable.ic_instagram
    KrailSocialType.Facebook -> Res.drawable.ic_facebook
}

fun SocialType.resource(): DrawableResource = when (this) {
    LinkedIn -> Res.drawable.ic_linkedin
    Reddit -> Res.drawable.ic_reddit
    Instagram -> Res.drawable.ic_instagram
    Facebook -> Res.drawable.ic_facebook
}

fun KrailSocialType.toAnalyticsEventPlatform(): SocialConnectionLinkClickEvent.SocialPlatform =
    when (this) {
        KrailSocialType.LinkedIn -> SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
        KrailSocialType.Reddit -> SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
        KrailSocialType.Instagram -> SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
        KrailSocialType.Facebook -> SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
    }
