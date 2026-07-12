package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Expand/collapse affordance on an unassigned stop-search-result row (`StopLabelAssignRow`).
 * Tapping it toggles the label wall open. Once a stop has a label, the row shows the label
 * pill itself (inline next to the transport-mode icons) instead of this icon — there is
 * no "assigned" state for this icon to render, and no way back into edit mode from the
 * row (that lives in Manage now), so this only ever toggles unassigned rows.
 *
 * Bare down-chevron (0 to 180 degree rotation on expand), no circle background —
 * a filled circle at touch-target scale read as too heavy inline in a row.
 */
@Composable
internal fun AssignToLabelIcon(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "assign_to_label_arrow_rotation",
    )

    Box(
        modifier = modifier
            .size(TOUCH_TARGET_SIZE)
            .clip(CircleShape)
            .klickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            tint = KrailTheme.colors.onSurface,
            contentDescription = if (expanded) "Collapse" else "Assign to a label",
            modifier = Modifier
                .size(dim.iconL)
                .rotate(rotationAngle),
        )
    }
}

private val TOUCH_TARGET_SIZE = 48.dp

// region Previews

@PreviewComponent
@Composable
private fun PreviewAssignToLabelIcon() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AssignToLabelIcon(expanded = false, onClick = {})
    }
}

@PreviewComponent
@Composable
private fun PreviewAssignToLabelIcon_Expanded() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AssignToLabelIcon(expanded = true, onClick = {})
    }
}

@PreviewComponent
@Composable
private fun PreviewAssignToLabelIcon_InRow() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        Row(
            modifier = Modifier.padding(KrailTheme.dimensions.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                xyz.ksharma.krail.taj.components.Text(
                    text = "Central Station",
                    style = KrailTheme.typography.titleLarge,
                    color = KrailTheme.colors.onSurface,
                )
                TransportModeIcon(transportMode = TransportMode.Train, size = TransportModeIconSize.Small)
            }
            AssignToLabelIcon(expanded = false, onClick = {})
        }
    }
}

// endregion
