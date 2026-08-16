package xyz.ksharma.krail.core.remoteconfig

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every flag needs a default, and a missing one cannot be spotted by reading either file.
 *
 * `AI_SEARCH_INPUT_ENABLED` shipped without a row here and nothing caught it: not the compiler,
 * not detekt, and not the feature working on a device, because a key Firebase has never heard of
 * returns an empty string, which reads as `false` and happened to be the value that flag wanted.
 * The trap is the flag that wants `true` — see the note on [RemoteConfigDefaults].
 */
class RemoteConfigDefaultsTest {

    @Test
    fun everyFlagKeyHasADefault() {
        val defaults = RemoteConfigDefaults.getDefaults().map { it.first }.toSet()
        val missing = FlagKeys.entries.map { it.key }.filterNot { it in defaults }

        assertTrue(
            missing.isEmpty(),
            "FlagKeys with no row in RemoteConfigDefaults.getDefaults(): $missing. " +
                "A key absent here reads as false for every client that has not completed a " +
                "fetch, whatever fallback the call site passes.",
        )
    }

    @Test
    fun noDefaultIsDeclaredTwice() {
        val keys = RemoteConfigDefaults.getDefaults().map { it.first }
        val duplicated = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        // Which of two rows wins is an ordering detail of the array, so a duplicate is a flag
        // whose value depends on where someone happened to paste it.
        assertTrue(duplicated.isEmpty(), "Duplicate default rows: $duplicated")
    }

    @Test
    fun noDefaultIsForAKeyThatNoLongerExists() {
        val flagKeys = FlagKeys.entries.map { it.key }.toSet()
        val orphans = RemoteConfigDefaults.getDefaults()
            .map { it.first }
            .filterNot { it in flagKeys }

        // A default left behind after its FlagKeys entry was deleted is dead weight that reads
        // as a live flag.
        assertEquals(emptyList(), orphans, "Defaults with no matching FlagKeys entry: $orphans")
    }
}
