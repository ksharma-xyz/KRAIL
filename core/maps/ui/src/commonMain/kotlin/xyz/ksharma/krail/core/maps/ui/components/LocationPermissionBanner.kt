package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.Res
import app.krail.taj.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Banner shown when location permission is permanently denied.
 *
 * Displays below search bar, above map area.
 * Matches provided background color (typically search bar color).
 *
 * Features:
 * - Clear message about permission requirement
 * - "Settings" button to open app settings
 * - Dismissible with close button
 * - Animated enter/exit
 *
 * @param permissionStatus Current permission status
 * @param onGoToSettings Callback to open app settings
 * @param onDismiss Callback to dismiss banner
 * @param backgroundColor Background color (typically from search bar)
 * @param modifier Modifier for the banner
 */
@Composable
fun LocationPermissionBanner(
    permissionStatus: PermissionStatus,
    onGoToSettings: () -> Unit,
    backgroundColor: Color = KrailTheme.colors.surface,
    modifier: Modifier = Modifier,
) {
    // Only show for permanent denial
    val isVisible = permissionStatus is PermissionStatus.Denied.Permanent

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = backgroundColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Location Permission Required",
                        style = KrailTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Enable location in Settings to see your position on the map.",
                        style = KrailTheme.typography.bodySmall,
                        color = KrailTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onGoToSettings) {
                        Text(
                            text = "Go to Settings",
                            style = KrailTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
