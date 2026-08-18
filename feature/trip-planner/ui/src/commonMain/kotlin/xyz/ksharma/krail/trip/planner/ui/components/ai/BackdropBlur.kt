package xyz.ksharma.krail.trip.planner.ui.components.ai

/**
 * Whether `Modifier.blur` actually draws on this device.
 *
 * On Android the modifier needs RenderEffect (API 31); below that it is a silent no-op, so a
 * surface relying on the blur to push the background out of focus has to fall back to a dim
 * instead. iOS renders through Skia, where the blur always draws.
 */
internal expect fun isBackdropBlurSupported(): Boolean
