package xyz.ksharma.krail.core.appinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS has no foldables and no fold API, so this is [FoldState.NONE] for every user,
 * always. Reported rather than omitted so the parameter is never missing.
 *
 * iPad multitasking needs nothing here: Split View and Slide Over shrink the window, so
 * they already show up in the width size class, which is the window the app actually got.
 */
@Composable
actual fun rememberDeviceFoldInfo(): DeviceFoldInfo = remember { DeviceFoldInfo() }
