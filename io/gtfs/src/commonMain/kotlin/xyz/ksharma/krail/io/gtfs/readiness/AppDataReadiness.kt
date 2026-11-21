package xyz.ksharma.krail.io.gtfs.readiness

import kotlinx.coroutines.flow.StateFlow

/** Whether the bundled stops data is in the database and usable. Bus routes are not part of it. */
sealed interface StopsReadiness {

    /** Nothing has run yet in this process. */
    data object Unknown : StopsReadiness

    /** The stops insert, or the automatic retries after a failed one, is in progress. */
    data object Loading : StopsReadiness

    /** The data is current, the table is populated and a known stop can be read back. */
    data class Ready(val stopCount: Int) : StopsReadiness

    /** [attempt] counts every insert attempt made in this process, not only the latest run. */
    data class Failed(val attempt: Int, val reason: String) : StopsReadiness
}

interface AppDataReadiness {

    val state: StateFlow<StopsReadiness>

    /**
     * Suspends until the stops are [StopsReadiness.Ready] or [StopsReadiness.Failed], or until
     * [timeoutMs] passes, and returns the state at that point. Returns as soon as the stops are
     * Ready: the bus routes insert follows in the background and is not awaited. Never throws; only
     * cancellation of the caller propagates. Starts the first run if nothing has started yet.
     */
    suspend fun awaitReady(timeoutMs: Long): StopsReadiness

    /**
     * A no-op while [StopsReadiness.Loading] or [StopsReadiness.Ready], so calling it again in a
     * live process never demotes good data. From [StopsReadiness.Failed] it is one more attempt.
     * From [StopsReadiness.Unknown] it is the launch trigger and gets the automatic retries.
     */
    fun retry()
}
