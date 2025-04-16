package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_android_share
import krail.feature.trip_planner.ui.generated.resources.ic_ios_share
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.getAppPlatformType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun IntroContentInviteFriends(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TRIPS ARE\nBETTER\nWITH FRIENDS",
                style = KrailTheme.typography.introTagline,
                color = style.hexToComposeColor(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = style.hexToComposeColor())
                    .clickable(
                        indication = null,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onShareClick
                    ),
                contentAlignment = Alignment.Center,
            ) { // TODO - show diff. image for ios / android
                Image(
                    painter = if (getAppPlatformType() == DevicePlatformType.IOS) {
                        painterResource(Res.drawable.ic_ios_share)
                    } else {
                        painterResource(Res.drawable.ic_android_share)
                    },
                    contentDescription = "Invite Friends",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "\uD83D\uDC95",
            tagColor = style.hexToComposeColor()
        )
    }
}
