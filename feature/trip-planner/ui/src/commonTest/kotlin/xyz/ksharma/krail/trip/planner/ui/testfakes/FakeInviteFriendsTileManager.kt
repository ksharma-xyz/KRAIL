package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager

internal class FakeInviteFriendsTileManager : InviteFriendsTileManager {

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
