package xyz.ksharma.krail.taj.motion

import androidx.compose.runtime.Composable

/**
 * Whether the rider has asked the OS for less motion: iOS Reduce Motion, or Android's animator
 * duration scale switched off. For ambient movement that carries no information, which should
 * stop rather than merely slow down.
 */
@Composable
expect fun isReduceMotionEnabled(): Boolean
