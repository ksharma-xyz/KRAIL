package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.PlatformType
import xyz.ksharma.krail.taj.getAppPlatformType
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun TitleBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onNavActionClick: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(end = 16.dp, start = 8.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onNavActionClick?.let {
            NavActionButton(
                icon = if (getAppPlatformType() == PlatformType.IOS) {
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                },
                iconContentDescription = "Back",
                onClick = onNavActionClick,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            CompositionLocalProvider(
                LocalTextColor provides KrailTheme.colors.onSurface,
                LocalTextStyle provides KrailTheme.typography.headlineMedium,
            ) {
                title()
            }
        }
        actions?.let {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompositionLocalProvider(
                    LocalContainerColor provides KrailTheme.colors.onSurface,
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * A composable button with a circular shape, typically used for navigation actions.
 *
 * @param iconRes The [Painter] resource for the button's icon.
 * @param iconContentDescription A description of the icon for accessibility purposes.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 */
@Composable
fun NavActionButton(
    icon: ImageVector,
    iconContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .klickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.contentDescription = iconContentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
            modifier = Modifier.size(24.dp),
        )
    }
}
