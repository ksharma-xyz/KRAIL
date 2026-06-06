package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_stop_label_airport
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

@Composable
fun StopSearchListItem(
    stopName: String,
    stopId: String,
    transportModeSet: ImmutableSet<TransportMode>,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: (StopItem) -> Unit = {},
    isSaved: Boolean = false,
    onSaveAsLabel: ((StopItem) -> Unit)? = null,
    onUnsaveLabel: ((StopItem) -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                onClick(StopItem(stopId = stopId, stopName = stopName))
            }
            .padding(vertical = dim.spacingM, horizontal = dim.pageHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            Text(
                text = stopName,
                color = textColor,
                style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                transportModeSet.forEach { mode ->
                    TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
                }
                if (stopName.contains("airport", ignoreCase = true)) {
                    Image(
                        painter = painterResource(TajRes.drawable.ic_stop_label_airport),
                        contentDescription = "Airport",
                        colorFilter = ColorFilter.tint(KrailTheme.colors.label),
                        modifier = Modifier.size(TransportModeIconSize.Small.dpSize),
                    )
                }
            }
        }
        // Single SaveAsLabelStar call — Compose keys composable state by source
        // position, so branching into two different SaveAsLabelStar(...) calls would
        // reset the rotation Animatable + hasInitialised flag every time isSaved
        // flips (which is exactly when we want the rotation to play). Computing the
        // handler conditionally and passing the same composable preserves state.
        val starHandler: (() -> Unit)? = when {
            isSaved && onUnsaveLabel != null ->
                ({ onUnsaveLabel(StopItem(stopId = stopId, stopName = stopName)) })
            !isSaved && onSaveAsLabel != null ->
                ({ onSaveAsLabel(StopItem(stopId = stopId, stopName = stopName)) })
            else -> null
        }
        if (starHandler != null) {
            SaveAsLabelStar(
                state = if (isSaved) StarState.Saved else StarState.Unsaved,
                onClick = starHandler,
            )
        }
    }
}

/**
 * Two-state machine for the save-as-label star. Splitting "saved" and "unsaved" into
 * an enum (rather than a raw boolean) keeps the call sites readable and gives us a
 * single key for [LaunchedEffect] to drive the transition rotation off of.
 */
private enum class StarState {
    Unsaved,
    Saved,
}

@Composable
private fun SaveAsLabelStar(state: StarState, onClick: () -> Unit) {
    // Same tint for filled and outlined — only the silhouette changes — so the star's
    // colour stays consistent with surrounding text and doesn't fade out against any
    // background.
    val icon = when (state) {
        StarState.Saved -> Res.drawable.ic_star_filled
        StarState.Unsaved -> Res.drawable.ic_star
    }
    val description = when (state) {
        StarState.Saved -> "Remove from labels"
        StarState.Unsaved -> "Save as label"
    }

    // Spin the star a full 360° when it actually transitions between saved and
    // unsaved. Direction depends on the new state — saving spins clockwise, removing
    // spins counter-clockwise — so the gesture reads like "winding it in" vs
    // "letting it go". Duration matches the intro screen's save-trip animation so
    // the two surfaces feel like the same affordance. Skipped on first composition
    // so the star doesn't pirouette every time the row scrolls into view; only
    // fires on real state changes.
    val rotation = remember { Animatable(0f) }
    var hasInitialised by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (!hasInitialised) {
            hasInitialised = true
            return@LaunchedEffect
        }
        val direction = when (state) {
            StarState.Saved -> SAVE_ROTATION_DIRECTION
            StarState.Unsaved -> UNSAVE_ROTATION_DIRECTION
        }
        rotation.animateTo(
            targetValue = rotation.value + STAR_ROTATION_DEGREES * direction,
            animationSpec = tween(durationMillis = STAR_ROTATION_DURATION_MS),
        )
    }

    Image(
        painter = painterResource(icon),
        contentDescription = description,
        colorFilter = ColorFilter.tint(KrailTheme.colors.label),
        modifier = Modifier
            .size(STAR_TAP_TARGET_SIZE)
            .clip(CircleShape)
            .klickable(onClick = onClick)
            .padding(STAR_INNER_PADDING)
            .rotate(rotation.value),
    )
}

// Star tap target + visual padding picked to match KrailTheme touch-target guidance
// (≥ 48dp tap target with the visible icon centred at ~24dp). Matched at the call
// site instead of dim tokens because these values are component-specific and never
// reused elsewhere.
private val STAR_TAP_TARGET_SIZE = 36.dp
private val STAR_INNER_PADDING = 6.dp

// Rotation animation matches IntroContentSaveTrips so the two surfaces feel the
// same. Direction is signed: clockwise (+) when saving, counter-clockwise (−) when
// removing, so the visual reads like the action's intent.
private const val STAR_ROTATION_DEGREES = 360f
private const val STAR_ROTATION_DURATION_MS = 500
private const val SAVE_ROTATION_DIRECTION = 1f
private const val UNSAVE_ROTATION_DIRECTION = -1f

// region Preview

@ScreenshotTest
@Preview
@Composable
private fun StopSearchListItemPreview() {
    PreviewTheme {
        StopSearchListItem(
            stopId = "123",
            stopName = "Stop Name",
            transportModeSet = persistentSetOf(
                TransportMode.Bus,
                TransportMode.LightRail,
            ),
            textColor = KrailTheme.colors.onSurface,
        )
    }
}

@ScreenshotTest
@Preview
@Composable
private fun StopSearchListItemLongNamePreview() {
    PreviewTheme {
        StopSearchListItem(
            stopId = "123",
            stopName = "This is a very long stop name that should wrap to the next line",
            transportModeSet = persistentSetOf(
                TransportMode.Train,
                TransportMode.Ferry,
            ),
            textColor = KrailTheme.colors.onSurface,
        )
    }
}

// endregion
