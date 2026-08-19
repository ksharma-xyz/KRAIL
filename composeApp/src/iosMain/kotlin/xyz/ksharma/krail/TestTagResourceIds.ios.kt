package xyz.ksharma.krail

import androidx.compose.ui.Modifier

/**
 * No-op on iOS: Compose Multiplatform maps `Modifier.testTag` onto the element's
 * `accessibilityIdentifier` already, which is what XCUITest and Maestro read.
 */
actual fun Modifier.exposeTestTagsToUiAutomation(): Modifier = this
