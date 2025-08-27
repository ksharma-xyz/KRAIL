package xyz.ksharma.core.test.db

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import xyz.ksharma.core.test.fakes.FakeSandookPreferences
import xyz.ksharma.krail.info.tile.network.api.db.isInfoTileDismissed
import xyz.ksharma.krail.info.tile.network.api.db.markInfoTileAsDismissed

class InfoTilePreferencesTest {

    @Test
    fun `markInfoTileAsDismissed adds key and isInfoTileDismissed returns true`() {
        val prefs = FakeSandookPreferences()
        assertFalse(prefs.isInfoTileDismissed("tile1"))
        prefs.markInfoTileAsDismissed("tile1")
        assertTrue(prefs.isInfoTileDismissed("tile1"))
    }

    @Test
    fun `multiple keys are stored as comma separated and checked individually`() {
        val prefs = FakeSandookPreferences()
        prefs.markInfoTileAsDismissed("tile1")
        prefs.markInfoTileAsDismissed("tile2")
        assertTrue(prefs.isInfoTileDismissed("tile1"))
        assertTrue(prefs.isInfoTileDismissed("tile2"))
        assertFalse(prefs.isInfoTileDismissed("tile3"))
    }
}
