package xyz.ksharma.krail.core.maps.ui.config

import androidx.compose.ui.Alignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Attribution is a licence obligation, not a display preference.
 *
 * The tiles in [MapTileProvider.OPEN_FREE_MAP_LIBERTY] are built from OpenStreetMap data, and
 * the ODbL requires visible credit to OpenStreetMap contributors wherever the map is shown.
 * OpenFreeMap asks for credit of its own.
 *
 * This exists because it shipped switched off, alongside a disabled logo, in a block of ornament
 * flags where nothing distinguished the one that is required from the ones that are not. Every
 * map in the app drew OSM data with no credit anywhere, and there is no licences screen to carry
 * it instead. The failure was silent and stayed that way.
 *
 * A future tidy-up of those ornaments has to trip over this test.
 */
class MapAttributionTest {

    @Test
    fun `attribution is enabled, because the tile licence requires it`() {
        assertTrue(
            MapConfig.Ornaments.ATTRIBUTION_ENABLED,
            "OpenStreetMap tiles are shown without credit. This is a licence breach, " +
                "not a styling choice. See MapAttributionTest's KDoc before changing it.",
        )
    }

    @Test
    fun `attribution sits in the bottom left`() {
        // The corner the OSM community conventionally uses, and the one least likely to collide:
        // the compass and scale bar take the top corners, and bottom right is where a floating
        // action button goes.
        assertEquals(Alignment.BottomStart, MapConfig.Ornaments.ATTRIBUTION_ALIGNMENT)
    }

    @Test
    fun `the tile style is still the one this obligation was checked against`() {
        // Attribution requirements follow the tile source. A different provider means the
        // credit has to be re-checked rather than assumed to carry over.
        assertEquals(
            "https://tiles.openfreemap.org/styles/liberty",
            MapTileProvider.DEFAULT,
        )
    }
}
