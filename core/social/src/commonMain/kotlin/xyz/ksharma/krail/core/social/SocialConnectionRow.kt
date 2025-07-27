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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.social.model.KrailSocialType
import xyz.ksharma.krail.core.social.model.resource
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
