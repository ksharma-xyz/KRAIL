package xyz.ksharma.krail.core.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import xyz.ksharma.krail.core.log.log as krailLog

/**
 * Logcat / iOS console prefix for connectivity transitions. Filter the firehose
 * with `adb logcat | grep KrailConnectivity` on Android, or the Xcode console
 * search field.
 *
 * Matches the `KrailNetwork:` convention in `:core:network`: a plain message
 * prefix rather than a logcat tag, because KRAIL's logger derives its own tag from
 * the calling class.
 */
internal const val KRAIL_CONNECTIVITY_LOG_TAG: String = "KrailConnectivity:"

/**
 * Logs every transport transition.
 *
 * This is the only runtime signal the observer produces, and it is what makes the
 * platform implementations verifiable at all: they are thin wrappers over OS
 * callbacks that no unit test can reach, so the way to confirm one works is to
 * toggle airplane mode and read the log.
 *
 * Placed before `stateIn` in each implementation so it sees the raw callback
 * stream after de-duplication, not the shared downstream.
 */
internal fun Flow<TransportState>.logTransitions(): Flow<TransportState> =
    onEach { state -> krailLog("$KRAIL_CONNECTIVITY_LOG_TAG transport=$state") }
