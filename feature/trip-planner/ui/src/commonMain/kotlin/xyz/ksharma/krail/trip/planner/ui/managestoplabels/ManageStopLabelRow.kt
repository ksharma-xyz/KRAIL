package xyz.ksharma.krail.trip.planner.ui.managestoplabels

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.components.ConfirmLabelActionSheet
import xyz.ksharma.krail.trip.planner.ui.components.LABEL_NAME_MAX_LENGTH
import xyz.ksharma.krail.trip.planner.ui.components.filterLabelNameInput
import xyz.ksharma.krail.trip.planner.ui.components.normaliseLabelName
import xyz.ksharma.krail.trip.planner.ui.components.stopLabelColor
import xyz.ksharma.krail.trip.planner.ui.components.stopLabelIcon
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * Tap the whole row to expand it — same `animateContentSize` pattern as
 * `StopLabelAssignRow`/`JourneyCard` — revealing a rename field and Save/Remove/
 * Delete actions. A trailing down-chevron-in-solid-circle (same convention as
 * `TripSearchListItem`/`AssignToLabelIcon`, rotating 0 to 180 degrees on expand)
 * hints that the row is tappable — not a pencil+kebab pair, which read as two
 * different-looking buttons for one row.
 *
 * Reorder is NOT part of this per-row expand — it's a separate screen-level mode
 * (toggle in `ManageStopLabelsScreen`'s top bar) so dragging a row never fights with
 * tapping it to rename. While dragging, [isDragging] lifts the row into a small
 * elevated "card" (shadow + scale) so it's obvious it's been picked up.
 */
@Composable
internal fun ManageStopLabelRow(
    label: StopLabel,
    isReorderMode: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onRename: (newName: String) -> Unit,
    onRemoveAssignment: () -> Unit,
    onDeleteLabel: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    isDragging: Boolean = false,
    existingLabelNames: ImmutableList<String> = persistentListOf(),
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusM)

    val elevation by animateDpAsState(if (isDragging) dim.spacingXS else dim.spacingNone)
    val scale by animateFloatAsState(if (isDragging) DRAG_SCALE else 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, shape = shape, clip = false)
            // Every row gets a fill distinct from the screen's own surface
            // background — without it the row is fully transparent and just shows
            // the screen behind it. Collapsed rows use a plain neutral card colour
            // (edge-to-edge, no corner radius — reads as a flat list row); expanded
            // uses the same flat themeBackgroundColor() fill as SavedTripCard, with
            // rounded corners so the "activated" row visually stands out from its
            // neighbours.
            .then(
                if (expanded && !isReorderMode) {
                    Modifier.background(themeBackgroundColor(), shape)
                } else {
                    Modifier.background(KrailTheme.colors.discoverCardBackground)
                },
            )
            .then(if (isReorderMode) Modifier else Modifier.klickable { onExpandToggle() })
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL)
            .animateContentSize(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                // White icon on a per-label colour circle — Home is always orange,
                // Beach is always a cool teal, a custom label gets a stable (not
                // per-render random) colour hashed from its name.
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .size(dim.spacingXXXXL)
                    .clip(CircleShape)
                    .background(stopLabelColor(label.label), CircleShape)
                    .padding(dim.spacingXS),
            )

            // Label name is the row's primary identity — same titleLarge weight a
            // stop name gets elsewhere in this screen — with the assigned stop as a
            // clearly secondary line underneath.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
            ) {
                Text(
                    text = label.label,
                    style = KrailTheme.typography.titleLarge,
                    color = KrailTheme.colors.onSurface,
                )
                Text(
                    text = label.stopName ?: "Not Assigned",
                    style = KrailTheme.typography.bodyMedium,
                    // softLabel is a low-contrast muted gray, tuned for the plain
                    // surface background — unreadable once the row is expanded and
                    // sitting on the themed tint. onSurface stays legible either way.
                    color = if (expanded) KrailTheme.colors.onSurface else KrailTheme.colors.softLabel,
                )
            }

            if (isReorderMode) {
                // Bigger + pulled in from the screen edge with end padding — the
                // touch target was previously flush against the corner, easy to
                // miss/mis-tap.
                Text(
                    text = "⋮⋮",
                    style = KrailTheme.typography.titleLarge,
                    color = KrailTheme.colors.softLabel,
                    modifier = Modifier.padding(end = dim.spacingS).then(dragHandleModifier),
                )
            } else {
                // Bare chevron, not a pencil+kebab pair — hints the row expands
                // without reintroducing the two-affordance confusion that pattern
                // caused. No circle background — reads too heavy inline in a row.
                // Same rotating chevron convention as
                // TripSearchListItem/AssignToLabelIcon.
                val rotationAngle by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "manage_stop_label_row_arrow_rotation",
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = KrailTheme.colors.softLabel,
                    modifier = Modifier
                        .size(dim.iconL)
                        .rotate(rotationAngle),
                )
            }
        }

        if (expanded && !isReorderMode) {
            ExpandedRenameContent(
                currentName = label.label,
                isSet = label.isSet,
                isProtected = label.isProtected,
                existingLabelNames = existingLabelNames,
                onRename = onRename,
                onRemoveAssignment = onRemoveAssignment,
                onDeleteLabel = onDeleteLabel,
                onCollapse = onExpandToggle,
            )
        }
    }
}

private const val DRAG_SCALE = 1.02f

/** Result of tapping "Save changes" in [ExpandedRenameContent]'s rename field. */
internal sealed interface RenameSaveAction {
    /** Blank or unchanged — nothing to save, just collapse the row. */
    data object CollapseOnly : RenameSaveAction

    /** Normalises to the same name as another existing label — block with a sheet. */
    data class Duplicate(val cleanedName: String) : RenameSaveAction

    /** A genuinely new, non-duplicate name — proceed with the rename. */
    data class Save(val trimmedName: String) : RenameSaveAction
}

internal fun resolveRenameSaveAction(
    typedName: String,
    currentName: String,
    existingLabelNames: List<String>,
): RenameSaveAction {
    val trimmed = typedName.trim()
    if (trimmed.isBlank() || trimmed == currentName) return RenameSaveAction.CollapseOnly
    val cleaned = normaliseLabelName(trimmed)
    val duplicate = existingLabelNames.any {
        !it.equals(currentName, ignoreCase = true) &&
            normaliseLabelName(it).equals(cleaned, ignoreCase = true)
    }
    return if (duplicate) RenameSaveAction.Duplicate(cleaned) else RenameSaveAction.Save(trimmed)
}

@Composable
private fun ExpandedRenameContent(
    currentName: String,
    isSet: Boolean,
    isProtected: Boolean,
    onRename: (String) -> Unit,
    onRemoveAssignment: () -> Unit,
    onDeleteLabel: () -> Unit,
    onCollapse: () -> Unit,
    existingLabelNames: ImmutableList<String> = persistentListOf(),
) {
    val dim = KrailTheme.dimensions
    val textFieldState = remember { TextFieldState(currentName) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var duplicateName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(top = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingS),
    ) {
        // Home is the permanent shortcut — renaming it away from "Home" would also
        // un-protect it (isProtected is name-based), silently making it deletable
        // and orphaning the "always-on Home shortcut" invariant. Simplest fix:
        // Home can't be renamed at all, only reassigned/cleared.
        if (!isProtected) {
            Text(
                text = "Rename",
                style = KrailTheme.typography.titleMedium,
                // onSurface, not softLabel — this content only ever renders against
                // the row's themed tint background, where softLabel's low contrast
                // reads as unreadable.
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(top = dim.spacingXL),
            )
            TextField(
                placeholder = "e.g. Home, Gym, School…",
                state = textFieldState,
                // Same allowlist as AssignNewLabelSheet's create field — blocks
                // emoji/punctuation at keystroke time instead of silently stripping
                // them later, so raw and normalised never diverge here either.
                filter = ::filterLabelNameInput,
                maxLength = LABEL_NAME_MAX_LENGTH,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }

        // Save is the expected action, so it gets its own full-width row; Remove
        // assignment / Delete label are secondary and destructive, so they're
        // demoted into a smaller side-by-side row underneath instead of stacking
        // three full-width buttons.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dim.spacingXXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingL),
        ) {
            if (!isProtected) {
                Button(
                    onClick = {
                        when (
                            val action = resolveRenameSaveAction(
                                typedName = textFieldState.text.toString(),
                                currentName = currentName,
                                existingLabelNames = existingLabelNames,
                            )
                        ) {
                            RenameSaveAction.CollapseOnly -> {
                                focusManager.clearFocus()
                                onCollapse()
                            }

                            is RenameSaveAction.Duplicate -> {
                                // Don't collapse — the ViewModel's own dedupe check is
                                // a silent no-op, so collapsing here would close the
                                // row while implying the rename succeeded when
                                // nothing was saved.
                                duplicateName = action.cleanedName
                            }

                            is RenameSaveAction.Save -> {
                                focusManager.clearFocus()
                                onRename(action.trimmedName)
                                onCollapse()
                            }
                        }
                    },
                    colors = ButtonDefaults.monochromeButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Save changes")
                }
            }

            if (isSet || !isProtected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                ) {
                    if (isSet) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onRemoveAssignment()
                                onCollapse()
                            },
                            dimensions = ButtonDefaults.largeButtonSize(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Remove")
                        }
                    }

                    if (!isProtected) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onDeleteLabel()
                                onCollapse()
                            },
                            colors = ButtonDefaults.destructiveButtonColors(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Delete")
                        }
                    }
                }
            }
        }
    }

    val blockedName = duplicateName
    if (blockedName != null) {
        ConfirmLabelActionSheet(
            title = "\"$blockedName\" already exists",
            message = "Choose a different name for this label.",
            primaryText = "OK",
            onPrimary = { duplicateName = null },
            onCancel = { duplicateName = null },
        )
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewManageStopLabelRow_HomeCollapsed() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ManageStopLabelRow(
            label = StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
            isReorderMode = false,
            expanded = false,
            onExpandToggle = {},
            onRename = {},
            onRemoveAssignment = {},
            onDeleteLabel = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewManageStopLabelRow_HomeExpanded() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        ManageStopLabelRow(
            label = StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
            isReorderMode = false,
            expanded = true,
            onExpandToggle = {},
            onRename = {},
            onRemoveAssignment = {},
            onDeleteLabel = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewManageStopLabelRow_CustomExpanded() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        ManageStopLabelRow(
            label = StopLabel(emoji = "🏋", label = "Gym", stopId = "2", stopName = "Bondi Junction"),
            isReorderMode = false,
            expanded = true,
            onExpandToggle = {},
            onRename = {},
            onRemoveAssignment = {},
            onDeleteLabel = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewManageStopLabelRow_ReorderMode() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        ManageStopLabelRow(
            label = StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
            isReorderMode = true,
            expanded = false,
            onExpandToggle = {},
            onRename = {},
            onRemoveAssignment = {},
            onDeleteLabel = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewManageStopLabelRow_Dragging() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        ManageStopLabelRow(
            label = StopLabel(emoji = "🏋", label = "Gym", stopId = "2", stopName = "Bondi Junction"),
            isReorderMode = true,
            expanded = false,
            onExpandToggle = {},
            onRename = {},
            onRemoveAssignment = {},
            onDeleteLabel = {},
            isDragging = true,
        )
    }
}

// endregion
