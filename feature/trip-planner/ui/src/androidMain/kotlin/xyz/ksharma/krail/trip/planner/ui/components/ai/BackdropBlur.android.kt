package xyz.ksharma.krail.trip.planner.ui.components.ai

import android.os.Build

internal actual fun isBackdropBlurSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
