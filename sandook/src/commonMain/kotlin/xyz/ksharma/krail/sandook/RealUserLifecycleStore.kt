package xyz.ksharma.krail.sandook

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Counters live in their own table because each one carries two values (a total and a
 * last-seen time). The first-install stamp is a lone scalar, so it reuses the existing
 * [SandookPreferences] key-value table rather than earning a table of its own.
 *
 * @param nowMillis injectable clock; production uses the system clock, tests drive it.
 */
internal class RealUserLifecycleStore(
    private val queries: UserLifecycleQueries,
    private val preferences: SandookPreferences,
    private val nowMillis: () -> Long = { systemNowMillis() },
) : UserLifecycleStore {

    override fun recordFirstInstallIfAbsent() {
        if (preferences.getLong(KEY_FIRST_INSTALL_AT_MILLIS) != null) return
        preferences.setLong(KEY_FIRST_INSTALL_AT_MILLIS, nowMillis())
    }

    override fun firstInstallAtMillis(): Long? = preferences.getLong(KEY_FIRST_INSTALL_AT_MILLIS)

    override fun daysSinceFirstInstall(): Long? {
        val installedAtMillis = firstInstallAtMillis() ?: return null
        val elapsedMillis = nowMillis() - installedAtMillis
        // A device clock moved backwards would otherwise make the install look "not yet made".
        return if (elapsedMillis <= 0) 0L else elapsedMillis / MILLIS_PER_DAY
    }

    override fun increment(counter: LifecycleCounter): Long =
        queries.transactionWithResult {
            queries.incrementCounter(key = counter.key, lastAtMillis = nowMillis())
            queries.selectCounter(counter.key).executeAsOne().event_count
        }

    override fun count(counter: LifecycleCounter): Long =
        queries.selectCounter(counter.key).executeAsOneOrNull()?.event_count ?: 0L

    override fun lastAtMillis(counter: LifecycleCounter): Long? =
        queries.selectCounter(counter.key).executeAsOneOrNull()?.last_at_millis

    override fun millisSinceLast(counter: LifecycleCounter): Long? {
        val lastAt = lastAtMillis(counter) ?: return null
        return nowMillis() - lastAt
    }

    override fun reset(counter: LifecycleCounter) {
        queries.deleteCounter(counter.key)
    }

    companion object {

        /**
         * Stored in [SandookPreferences] rather than [UserLifecycleCounter] — it is a single
         * scalar, and KrailPref already survives updates the same way.
         */
        const val KEY_FIRST_INSTALL_AT_MILLIS = "KEY_FIRST_INSTALL_AT_MILLIS"

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        @OptIn(ExperimentalTime::class)
        private fun systemNowMillis(): Long = Clock.System.now().toEpochMilliseconds()
    }
}
