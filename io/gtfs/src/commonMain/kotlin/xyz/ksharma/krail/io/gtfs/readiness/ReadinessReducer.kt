package xyz.ksharma.krail.io.gtfs.readiness

internal const val REASON_VERSION_OLD = "Stored stops version is older than the bundled data"
internal const val REASON_COUNT_LOW = "Stop count is below the minimum"
internal const val REASON_CANARY_MISSING = "A known stop could not be read back"
internal const val REASON_RUN_INTERRUPTED = "The insert run was cancelled before it finished"

/** What is known about one run of the guarded insert and the database after it. */
internal sealed interface ReadinessInput {
    data object NotStarted : ReadinessInput
    data object Running : ReadinessInput
    data class InsertFailed(val reason: String) : ReadinessInput

    /** The insert returned success. The rest is what the database says afterwards. */
    data class InsertDone(
        val storedVersion: Long,
        val expectedVersion: Long,
        val stopCount: Int,
        val minimumStopCount: Int,
        val canaryFound: Boolean,
    ) : ReadinessInput
}

/**
 * The one place that decides whether stops are usable. The preference flag alone is not trusted:
 * the version, the row count and a read of one known stop must all agree. A stored version newer
 * than the bundled one counts as current, the same way the insert guard treats it, so the two can
 * never disagree and leave a state no retry can clear.
 */
internal fun reduceReadiness(input: ReadinessInput, attempt: Int): StopsReadiness = when (input) {
    ReadinessInput.NotStarted -> StopsReadiness.Unknown
    ReadinessInput.Running -> StopsReadiness.Loading
    is ReadinessInput.InsertFailed -> StopsReadiness.Failed(attempt, input.reason)
    is ReadinessInput.InsertDone -> reduceAfterInsert(input, attempt)
}

private fun reduceAfterInsert(done: ReadinessInput.InsertDone, attempt: Int): StopsReadiness {
    val problem = when {
        done.storedVersion < done.expectedVersion -> REASON_VERSION_OLD
        done.stopCount < done.minimumStopCount -> REASON_COUNT_LOW
        !done.canaryFound -> REASON_CANARY_MISSING
        else -> null
    }
    return if (problem == null) {
        StopsReadiness.Ready(done.stopCount)
    } else {
        StopsReadiness.Failed(attempt, problem)
    }
}
