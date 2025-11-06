package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Manager interface to handle invite friends tile state with in-memory caching.
 */
interface InviteFriendsTileManager {
    /**
     * Returns whether the user has seen the invite friends tile.
     * Loads from preferences only once, then caches the value.
     */
    fun hasSeenInviteFriendsTile(): Boolean

    /**
     * Marks the invite friends tile as seen.
     * Updates both the preference and the cached value.
     */
    fun markAsSeen()
}

/**
 * Real implementation of [InviteFriendsTileManager].
 * Loads the preference value once and caches it to avoid repeated preference reads.
 */
internal class RealInviteFriendsTileManager(
    private val preferences: SandookPreferences,
) : InviteFriendsTileManager {
    private var hasSeenTile: Boolean? = null

    override fun hasSeenInviteFriendsTile(): Boolean {
        if (hasSeenTile == null) {
            hasSeenTile = preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INVITE_FRIENDS_TILE) ?: false
        }
        return hasSeenTile ?: false
    }

    override fun markAsSeen() {
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INVITE_FRIENDS_TILE, true)
        hasSeenTile = true
    }
}
