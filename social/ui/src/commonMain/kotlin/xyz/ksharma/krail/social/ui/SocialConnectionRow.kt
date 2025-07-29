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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.Button.Social.PartnerSocial.PartnerSocialLink
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.taj.components.SocialConnectionIcon
import xyz.ksharma.krail.taj.theme.KrailTheme

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
                    contentDescription = "${socialType.socialType.displayName} Page for KRAIL App",
                    colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun SocialConnectionRow(
    socialPartnerName: String,
    partnerSocialLinks: List<PartnerSocialLink>,
    modifier: Modifier = Modifier,
    onClick: (PartnerSocialLink) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        partnerSocialLinks.forEach { socialType ->
            SocialConnectionIcon(
                onClick = { onClick(socialType) },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(resource = socialType.type.resource()),
                    contentDescription = "${socialType.type.displayName} Page for $socialPartnerName",
                    colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
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
            onClick = {},
        )
    }
}
