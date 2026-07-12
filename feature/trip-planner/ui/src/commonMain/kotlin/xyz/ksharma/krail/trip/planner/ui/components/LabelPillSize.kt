package xyz.ksharma.krail.trip.planner.ui.components

/**
 * Size axis shared by [SetLabelPill] and [UnsetLabelPill]. [Small] matches the
 * transport-mode icon badge's circle diameter exactly (font-scale-aware via
 * `toAdaptiveDecorativeIconSize()`) for inline display next to those icons — e.g.
 * `StopLabelAssignRow`'s "already assigned" pill. [Normal] is the standalone tappable
 * chip size used everywhere else (top pill bar, expand-wall pills).
 */
internal enum class LabelPillSize { Small, Normal }
