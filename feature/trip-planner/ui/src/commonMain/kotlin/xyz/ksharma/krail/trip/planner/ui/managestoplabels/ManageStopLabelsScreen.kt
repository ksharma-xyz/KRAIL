package xyz.ksharma.krail.trip.planner.ui.managestoplabels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * Full screen for rename / remove stop / delete label / reorder — a real nav
 * destination (`ManageStopLabelsRoute`), not a sheet. Google Maps "Your Places" /
 * Uber Settings > Saved Places shape: a dedicated screen for extended review/edit.
 *
 * Reorder is opt-in via the top-bar toggle, not always-on long-press-drag — same
 * "long-press has poor discoverability" reasoning that removed choose-mode from the
 * pill row applies here too. Drag only activates once "Reorder" is tapped; a banner
 * makes the mode switch obvious screen-wide (not just the per-row drag handles).
 * [onMove] fires per swap as the drag crosses each target's bounds (`reorderable`'s
 * `onDrag`), not once on drop — the handler must batch its persistence accordingly
 * (see `SearchStopViewModel.MoveLabelToIndex`'s `insertTransaction` wrapper).
 */
@Composable
internal fun ManageStopLabelsScreen(
    stopLabels: ImmutableList<StopLabel>,
    onBackClick: () -> Unit,
    onRename: (StopLabel, newName: String) -> Unit,
    onRemoveAssignment: (StopLabel) -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    onMove: (labelKey: String, targetLabelKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isReorderMode by remember { mutableStateOf(false) }
    // Screen-wide so only one row is ever open at a time — expanding a second row
    // collapses whichever was open, across both the set and not-set sections.
    var expandedLabelKey by rememberSaveable { mutableStateOf<String?>(null) }
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Keys, not raw LazyListItemInfo indices — the LazyColumn also holds
        // non-draggable items (reorder banner, "Not Assigned" header/rows), so a raw
        // index would be offset from the dragged item's actual position in
        // stopLabels. Guard toKey against those non-draggable rows too: only a
        // set label is a valid drop target.
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        if (stopLabels.firstOrNull { it.label == toKey }?.isSet != true) {
            return@rememberReorderableLazyListState
        }
        onMove(fromKey, toKey)
    }
    val existingLabelNames = stopLabels.map { it.label }.toImmutableList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        TitleBar(
            modifier = Modifier.fillMaxWidth(),
            onNavActionClick = onBackClick,
            title = { Text(text = "Manage your labels") },
            actions = {
                TextButton(onClick = { isReorderMode = !isReorderMode }) {
                    Text(if (isReorderMode) "Done" else "Reorder")
                }
            },
        )

        val setLabels = stopLabels.filter { it.isSet }
        val unsetLabels = stopLabels.filter { !it.isSet }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            // Bottom breathing room so the last row can scroll clear of the system
            // nav bar / gesture area instead of sitting flush behind it.
            contentPadding = PaddingValues(bottom = dim.spacingXXXXL),
        ) {
            // Only the TitleBar (nav back + Reorder action) stays fixed — everything
            // else, including the description and reorder banner, scrolls with the
            // list so a long label list doesn't leave the top text permanently
            // stuck taking up space.
            if (!isReorderMode) {
                item(key = "description") {
                    Text(
                        text = "Rename, reorder, or remove the stops saved under each label.",
                        style = KrailTheme.typography.bodyLarge,
                        color = KrailTheme.colors.softLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dim.pageHorizontalPadding)
                            .padding(top = dim.spacingXS, bottom = dim.spacingL)
                            .animateItem(),
                    )
                }
            }

            item(key = "reorder-banner") {
                // animateContentSize gives a smooth height collapse/expand matching
                // the pattern used for SearchStopScreen's contextual banner.
                Box(modifier = Modifier.animateContentSize().animateItem()) {
                    if (isReorderMode) {
                        ReorderModeBanner()
                    }
                }
            }

            items(items = setLabels, key = { it.label }) { label ->
                ReorderableItem(reorderState, key = label.label) { isDragging ->
                    ManageStopLabelRow(
                        label = label,
                        isReorderMode = isReorderMode,
                        expanded = expandedLabelKey == label.label,
                        onExpandToggle = {
                            expandedLabelKey = if (expandedLabelKey == label.label) null else label.label
                        },
                        isDragging = isDragging,
                        existingLabelNames = existingLabelNames,
                        onRename = { newName -> onRename(label, newName) },
                        onRemoveAssignment = { onRemoveAssignment(label) },
                        onDeleteLabel = { onDeleteLabel(label) },
                        dragHandleModifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        ),
                        // Without this, expanding/collapsing a row snaps the rows below
                        // into place instead of sliding — they'd flash out of order and
                        // briefly overlap while this row's own animateContentSize plays.
                        modifier = Modifier.animateItem(),
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
            }

            if (unsetLabels.isNotEmpty()) {
                item(key = "not-set-header") {
                    Text(
                        text = "Not Assigned",
                        style = KrailTheme.typography.titleMedium,
                        color = KrailTheme.colors.softLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = dim.pageHorizontalPadding, end = dim.pageHorizontalPadding)
                            .padding(top = dim.spacingXXL, bottom = dim.spacingM)
                            .animateItem(),
                    )
                }
            }

            items(items = unsetLabels, key = { "unset_${it.label}" }) { label ->
                // Not part of reorder — only set labels have a meaningful order
                // (they drive the top pill bar's display order); unset labels are
                // just waiting to be assigned, so dragging them is a no-op.
                ManageStopLabelRow(
                    label = label,
                    isReorderMode = false,
                    expanded = expandedLabelKey == label.label,
                    onExpandToggle = {
                        expandedLabelKey = if (expandedLabelKey == label.label) null else label.label
                    },
                    isDragging = false,
                    existingLabelNames = existingLabelNames,
                    onRename = { newName -> onRename(label, newName) },
                    onRemoveAssignment = { onRemoveAssignment(label) },
                    onDeleteLabel = { onDeleteLabel(label) },
                    modifier = Modifier.animateItem(),
                )
                Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
            }
        }
    }
}

@Composable
private fun ReorderModeBanner() {
    val dim = KrailTheme.dimensions
    Text(
        text = "Long press and then drag ⋮⋮ to reorder your labels",
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dim.pageHorizontalPadding,
                vertical = dim.spacingS,
            ),
    )
}

// region Previews

@PreviewScreen
@Composable
private fun PreviewManageStopLabelsScreen_Normal() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ManageStopLabelsScreen(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
                StopLabel(emoji = "💼", label = "Work"),
                StopLabel(emoji = "🏋", label = "Gym", stopId = "2", stopName = "Bondi Junction"),
                StopLabel(emoji = "☕", label = "Cafe"),
            ),
            onBackClick = {},
            onRename = { _, _ -> },
            onRemoveAssignment = {},
            onDeleteLabel = {},
            onMove = { _, _ -> },
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewManageStopLabelsScreen_FreshInstall() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        ManageStopLabelsScreen(
            stopLabels = StopLabel.defaults,
            onBackClick = {},
            onRename = { _, _ -> },
            onRemoveAssignment = {},
            onDeleteLabel = {},
            onMove = { _, _ -> },
        )
    }
}

// endregion
