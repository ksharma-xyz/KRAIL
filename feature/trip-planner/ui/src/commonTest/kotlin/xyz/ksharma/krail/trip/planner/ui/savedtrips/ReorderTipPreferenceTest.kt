package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_HAS_SEEN_INVITE_FRIENDS_TILE
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReorderTipPreferenceTest {

    private val prefs = InMemorySandookPreferences()

    @Test
    fun `Given preference not set When read Then tip is not seen`() {
        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP) ?: false
        assertFalse(hasSeen, "Tip should show when preference has never been written")
    }

    @Test
    fun `Given tip marked seen When read Then tip is seen`() {
        prefs.setBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP, true)

        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP) ?: false
        assertTrue(hasSeen, "Tip should be hidden after marking as seen")
    }

    @Test
    fun `Given tip marked seen When read multiple times Then always returns true`() {
        prefs.setBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP, true)

        repeat(3) { i ->
            assertTrue(
                prefs.getBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP) ?: false,
                "Read #$i should still return true after marking as seen",
            )
        }
    }

    @Test
    fun `Given preference key is unique Then it does not collide with other keys`() {
        prefs.setBoolean(KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP, true)

        assertFalse(
            prefs.getBoolean(KEY_HAS_SEEN_INVITE_FRIENDS_TILE) ?: false,
            "Reorder tip key must not bleed into invite-friends key",
        )
    }
}

private class InMemorySandookPreferences : SandookPreferences {
    private val store = mutableMapOf<String, Any>()
    override fun getLong(key: String) = store[key] as? Long
    override fun setLong(key: String, value: Long) { store[key] = value }
    override fun getString(key: String) = store[key] as? String
    override fun setString(key: String, value: String) { store[key] = value }
    override fun getBoolean(key: String) = store[key] as? Boolean
    override fun setBoolean(key: String, value: Boolean) { store[key] = value }
    override fun getDouble(key: String) = store[key] as? Double
    override fun setDouble(key: String, value: Double) { store[key] = value }
    override fun deletePreference(key: String) { store.remove(key) }
}
