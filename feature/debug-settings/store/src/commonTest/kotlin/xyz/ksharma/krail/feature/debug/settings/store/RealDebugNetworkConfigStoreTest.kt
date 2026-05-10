package xyz.ksharma.krail.feature.debug.settings.store

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.sandook.SandookPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [RealDebugNetworkConfigStore]. Covers hydration from defaults,
 * write-and-read round-tripping for [NetworkSource], the reset path, and
 * a corrupt-enum hydration fallback. Uses an in-memory [SandookPreferences]
 * fake to avoid any DB infrastructure in commonTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealDebugNetworkConfigStoreTest {

    @Test
    fun `Given fresh install When constructed Then state hydrates to defaults`() = runTest {
        val store = newStore()

        store.state.test {
            assertEquals(DebugSettingsState.default(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given default state When SetSource BFF_PROD Then store emits new source`() = runTest {
        val store = newStore()

        store.set(DebugSettingsEvent.SetSource(NetworkSource.BFF_PROD))

        assertEquals(NetworkSource.BFF_PROD, store.source())
    }

    @Test
    fun `Given default state When SetSource NSW_DIRECT Then store emits new source`() = runTest {
        val store = newStore()

        store.set(DebugSettingsEvent.SetSource(NetworkSource.NSW_DIRECT))

        assertEquals(NetworkSource.NSW_DIRECT, store.source())
    }

    @Test
    fun `Given mutated state When Reset Then source reverts to default`() = runTest {
        val store = newStore()
        store.set(DebugSettingsEvent.SetSource(NetworkSource.BFF_PROD))

        store.set(DebugSettingsEvent.Reset)

        assertEquals(DebugSettingsState.DEFAULT_SOURCE, store.source())
    }

    @Test
    fun `Given persisted prefs When new store constructed Then hydrates from prefs`() = runTest {
        val prefs = InMemorySandookPreferences()
        prefs.setString(
            RealDebugNetworkConfigStore.KEY_DEBUG_NETWORK_SOURCE,
            NetworkSource.BFF_PROD.name,
        )

        val store = RealDebugNetworkConfigStore(prefs, UnconfinedTestDispatcher())

        assertEquals(NetworkSource.BFF_PROD, store.source())
    }

    @Test
    fun `Given corrupt enum string When hydrating Then falls back to default`() = runTest {
        val prefs = InMemorySandookPreferences()
        prefs.setString(
            RealDebugNetworkConfigStore.KEY_DEBUG_NETWORK_SOURCE,
            "NOT_A_REAL_VALUE",
        )

        val store = RealDebugNetworkConfigStore(prefs, UnconfinedTestDispatcher())

        assertEquals(DebugSettingsState.DEFAULT_SOURCE, store.source())
    }

    private fun newStore(): RealDebugNetworkConfigStore = RealDebugNetworkConfigStore(
        preferences = InMemorySandookPreferences(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}

private class InMemorySandookPreferences : SandookPreferences {
    private val longs = mutableMapOf<String, Long>()
    private val strings = mutableMapOf<String, String>()
    private val bools = mutableMapOf<String, Boolean>()
    private val doubles = mutableMapOf<String, Double>()

    override fun getLong(key: String): Long? = longs[key]
    override fun setLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getString(key: String): String? = strings[key]
    override fun setString(key: String, value: String) {
        strings[key] = value
    }

    override fun getBoolean(key: String): Boolean? = bools[key]
    override fun setBoolean(key: String, value: Boolean) {
        bools[key] = value
    }

    override fun getDouble(key: String): Double? = doubles[key]
    override fun setDouble(key: String, value: Double) {
        doubles[key] = value
    }

    override fun deletePreference(key: String) {
        longs.remove(key)
        strings.remove(key)
        bools.remove(key)
        doubles.remove(key)
    }
}
