package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/** What the two floating rows say and do, in one holder so the block hands it over as one argument. */
@Immutable
internal data class FloatingRowsContent(
    val to: BarContent,
    val from: BarContent,
    val editing: ToFromField?,
    val editor: ToFromEditor?,
    val showFrom: Boolean,
    val firstLeading: @Composable () -> Unit,
    val toTrailing: (@Composable () -> Unit)?,
    val fromTrailing: (@Composable () -> Unit)?,
    val showGo: Boolean,
    val onGoClick: () -> Unit,
)
