package xyz.ksharma.krail.core.social

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
import xyz.ksharma.krail.taj.components.SocialConnectionIcon
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun SocialConnectionRow(
    modifier: Modifier = Modifier,
    onClick: (KrailSocialType) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KrailSocialType.entries.forEach { socialType ->
            SocialConnectionIcon(
                onClick = { onClick(socialType) },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(resource = socialType.resource()),
                    contentDescription = "${socialType.socialType} Page for KRAIL App",
                    colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
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

fun KrailSocialType.toAnalyticsEventPlatform(): SocialConnectionLinkClickEvent.SocialPlatform =
    when (this) {
        KrailSocialType.LinkedIn -> SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
        KrailSocialType.Reddit -> SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
        KrailSocialType.Instagram -> SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
        KrailSocialType.Facebook -> SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
    }
