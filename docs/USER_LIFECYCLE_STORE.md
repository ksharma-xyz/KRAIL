# User lifecycle store

`UserLifecycleStore` (in `:sandook`) is the one place that answers two questions features
keep asking:

- **How old is this install?** — `firstInstallAtMillis()`, `daysSinceFirstInstall()`
- **How many times has the user done X, and when last?** — `count()`, `lastAtMillis()`,
  `increment()`

Before this existed, any feature needing either had to invent its own persistence. Add to
this store instead of doing that again.

## State vs policy — keep them apart

The store holds **state**: facts about what actually happened on this device.

The **thresholds** those facts get compared against are **policy** and live in Remote Config
(`core/remote-config` `FlagKeys`), so they can change without shipping a release.

```
store.count(SAVED_TRIP_OPEN) >= remoteConfig.openCountThreshold   // ✅
store.count(SAVED_TRIP_OPEN) >= 3                                 // ❌ hardcoded policy
```

Never add a threshold constant to this store.

## Where it is persisted

| Fact | Storage |
|---|---|
| First-install time | `KrailPref` row, key `KEY_FIRST_INSTALL_AT_MILLIS` (a lone scalar, so it reuses the existing key-value table) |
| Counters | `UserLifecycleCounter` table — `key`, `event_count`, `last_at_millis` |

Both live in the app database (`krailSandook.db`), so they survive app updates and are not
cleared like a cache. Reads are synchronous and cheap enough to gate a launch-time or
tap-time decision.

## Adding a counter

Add a constant to the `LifecycleCounter` enum:

```kotlin
enum class LifecycleCounter(val key: String) {
    SAVED_TRIP_OPEN("saved_trip_open_count"),
    YOUR_NEW_COUNTER("your_new_counter"),
}
```

It is an enum rather than free-form strings so a typo cannot silently start a brand-new
counter, and so every counter in the app is visible in one place. No schema migration is
needed — rows are created on first `increment()`.

## Recording the install date

`RealAppStart` calls `recordFirstInstallIfAbsent()` on every launch. The first call stamps
the time; every call after that is a no-op, so the value is never overwritten.

**Caveat for users who upgraded into this:** their stamp is the first launch of the build
that contained the store, not their true install date. So an existing user reads as
brand-new for a while. Every gate built on `daysSinceFirstInstall()` is therefore
*conservative* — it waits longer than strictly needed, rather than firing early. Treat the
value as an "account age floor", not an exact age.

Firebase auto-logs `first_open`, but that value is not readable in-app; this store is what
in-app decision logic reads. The two are not interchangeable.

## Analytics

The store itself logs nothing. If a feature built on it needs a new event or param, follow
`docs/ANALYTICS_EVENTS.md` and add a row to `docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same
PR (Firebase caps the app at 500 unique event names, forever).

## Consumers

- In-app review prompt — reads `SAVED_TRIP_OPEN` and `daysSinceFirstInstall()`, compares both
  against Remote Config thresholds.
