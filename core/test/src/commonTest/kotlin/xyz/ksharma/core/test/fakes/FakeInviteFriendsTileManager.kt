package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager

class FakeInviteFriendsTileManager : InviteFriendsTileManager {

    private var hasSeenTile: Boolean = false

    override fun hasSeenInviteFriendsTile(): Boolean {
        return hasSeenTile
    }

    override fun markAsSeen() {
        hasSeenTile = true
    }

    // Test helper to reset state
    fun reset() {
        hasSeenTile = false
    }
}

