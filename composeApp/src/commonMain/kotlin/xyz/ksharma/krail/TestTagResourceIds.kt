package xyz.ksharma.krail

import androidx.compose.ui.Modifier

/**
 * Exposes every descendant's `Modifier.testTag` to the platform's UI-automation layer so
 * end-to-end drivers (Maestro) can select nodes by a stable id instead of by visible copy.
 *
 * Apply once, at the app root. It adds semantics only and never participates in layout or
 * drawing, so it cannot change a single pixel of output.
 *
 * - Android: sets `testTagsAsResourceId`, which surfaces each `testTag` as the
 *   accessibility node's `resource-id`.
 * - iOS: a no-op. Compose Multiplatform already publishes `testTag` as the
 *   `accessibilityIdentifier` of the corresponding element, so nothing extra is needed.
 */
expect fun Modifier.exposeTestTagsToUiAutomation(): Modifier
