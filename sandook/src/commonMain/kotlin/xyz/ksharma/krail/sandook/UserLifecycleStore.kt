package xyz.ksharma.krail.sandook

/**
 * Durable record of two things features keep needing to know: **how old this install is**
 * and **how many times the user has done a named thing**.
 *
 * Backed by the app database, so it survives app updates and is not cleared like a cache.
 * Reads are synchronous and cheap enough to gate a launch-time or tap-time decision.
 *
 * This store holds **state** (facts that happened). The thresholds those facts get compared
 * against (which open count, how many days old) are **policy** and belong in Remote Config so
 * they are tunable without a release. Do not add thresholds here.
 *
 * See `docs/USER_LIFECYCLE_STORE.md`.
 */
interface UserLifecycleStore {

    /**
     * Stamps the first-install time on the very first call and never overwrites it after.
     * Safe to call on every launch; call it before anything reads [firstInstallAtMillis].
     */
    fun recordFirstInstallIfAbsent()

    /**
     * Epoch millis of the first launch this store observed, or `null` if
     * [recordFirstInstallIfAbsent] has never run.
     *
     * Note this is the first launch *that had this store in the build*, which for users who
     * upgraded into it is later than their true install date. Treat it as "account age floor".
     */
    fun firstInstallAtMillis(): Long?

    /**
     * Whole days elapsed since [firstInstallAtMillis], or `null` if the install time was
     * never recorded. Same-day is `0`. Clamped at `0` so a backwards device clock cannot
     * produce a negative age.
     */
    fun daysSinceFirstInstall(): Long?

    /**
     * Bumps [counter] by one, stamps its last-seen time, and returns the new total.
     */
    fun increment(counter: LifecycleCounter): Long

    /** Times [counter] has been incremented; `0` if never. */
    fun count(counter: LifecycleCounter): Long

    /** Epoch millis of the most recent [increment] of [counter], or `null` if never. */
    fun lastAtMillis(counter: LifecycleCounter): Long?
}

/**
 * The named counters this store tracks.
 *
 * An enum rather than free-form strings so a typo cannot silently start a brand-new counter,
 * and so every counter in the app is visible in one place. Add a constant here rather than
 * inventing per-feature persistence.
 */
enum class LifecycleCounter(val key: String) {

    /** Incremented each time the user opens one of their saved trips. */
    SAVED_TRIP_OPEN("saved_trip_open_count"),

    /**
     * Incremented each time the platform review sheet is *requested*. The platform never
     * reports whether it was shown, so this counts asks, not ratings. Its last-seen time is
     * what enforces the cooldown between requests.
     */
    REVIEW_PROMPT_REQUESTED("review_prompt_requested_count"),
}
