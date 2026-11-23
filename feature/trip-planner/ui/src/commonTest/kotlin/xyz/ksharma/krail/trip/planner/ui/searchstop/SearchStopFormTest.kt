package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Search Stop form (rules 35 and 36) and the location pill (rule 24) with no layout. */
class SearchStopFormTest {

    private val central = StopItem(stopId = "200060", stopName = "Central Station")
    private val townHall = StopItem(stopId = "200070", stopName = "Town Hall Station")

    // region shape and placeholders

    @Test
    fun `a destination or a start is the To and From form, a label or one leg is one bar`() {
        assertEquals(FormShape.ToAndFrom, formShapeFor(SearchStopFieldType.TO, editTripLeg = false))
        assertEquals(FormShape.ToAndFrom, formShapeFor(SearchStopFieldType.FROM, editTripLeg = false))
        assertEquals(FormShape.Single, formShapeFor(SearchStopFieldType.LABEL, editTripLeg = false))
        assertEquals(FormShape.Single, formShapeFor(SearchStopFieldType.TO, editTripLeg = true))
        assertEquals(FormShape.Single, formShapeFor(SearchStopFieldType.FROM, editTripLeg = true))
    }

    @Test
    fun `the two bars read Choose destination and Choose starting point whichever field was opened`() {
        listOf(SearchStopFieldType.TO, SearchStopFieldType.FROM).forEach { type ->
            assertEquals("Choose destination", barPlaceholderFor(ToFromField.To, type, editTripLeg = false))
            assertEquals("Choose starting point", barPlaceholderFor(ToFromField.From, type, editTripLeg = false))
        }
    }

    @Test
    fun `the prompt is the fixed phrase of its bar and never a stop's name`() {
        // The function takes no stop, so nothing a bar holds can reach it, whether the field is empty or holds a stop,
        // focused or not. Every combination of field and opened-for field reads the same two phrases.
        for (type in listOf(SearchStopFieldType.TO, SearchStopFieldType.FROM)) {
            assertEquals(TO_PLACEHOLDER, barPlaceholderFor(ToFromField.To, type, editTripLeg = false))
            assertEquals(FROM_PLACEHOLDER, barPlaceholderFor(ToFromField.From, type, editTripLeg = false))
        }
        assertEquals("Choose destination", TO_PLACEHOLDER)
        assertEquals("Choose starting point", FROM_PLACEHOLDER)
    }

    @Test
    fun `the label field says Search and select with the label's saved name, exactly as saved`() {
        fun prompt(name: String?) = barPlaceholderFor(ToFromField.To, SearchStopFieldType.LABEL, false, labelName = name)

        assertEquals("Search and select Home", prompt("Home"))
        assertEquals("Search and select Work", prompt("Work"))
        assertEquals("Search and select Gym  Bay 2", prompt("Gym  Bay 2"), "A custom label is spelled as saved.")
        assertEquals("Search and select a stop", prompt(null), "No name to read still gives a whole prompt.")
        assertEquals("Search and select a stop", prompt("  "))
        assertEquals("Search and select Home", labelPromptFor("Home"))
    }

    @Test
    fun `a single bar names what it is for`() {
        assertEquals("Change origin", barPlaceholderFor(ToFromField.To, SearchStopFieldType.FROM, true))
        assertEquals("Change destination", barPlaceholderFor(ToFromField.To, SearchStopFieldType.TO, true))
    }

    // endregion

    // region go

    @Test
    fun `the go circle needs a start and a destination`() {
        assertTrue(goVisible(from = central, to = townHall))
        assertFalse(goVisible(from = null, to = townHall), "No start, no go circle.")
        assertFalse(goVisible(from = central, to = null))
        assertFalse(goVisible(from = null, to = null))
    }

    @Test
    fun `a trip from a stop to itself shows no go circle`() {
        assertFalse(goVisible(from = central, to = central))
    }

    // endregion

    // region which field a pick is for, and the auto start

    @Test
    fun `a pick is for the bar the rider was editing, whichever field the screen opened for`() {
        listOf(SearchStopFieldType.TO, SearchStopFieldType.FROM).forEach { opened ->
            assertEquals(
                SearchStopFieldType.TO,
                pickFieldFor(FormShape.ToAndFrom, ToFromField.To, opened),
            )
            assertEquals(
                SearchStopFieldType.FROM,
                pickFieldFor(FormShape.ToAndFrom, ToFromField.From, opened),
            )
        }
    }

    @Test
    fun `with nothing being edited a pick is for the destination`() {
        assertEquals(
            SearchStopFieldType.TO,
            pickFieldFor(FormShape.ToAndFrom, editing = null, fieldType = SearchStopFieldType.FROM),
        )
    }

    @Test
    fun `a single bar is always the field the route named`() {
        assertEquals(
            SearchStopFieldType.LABEL,
            pickFieldFor(FormShape.Single, ToFromField.To, SearchStopFieldType.LABEL),
        )
        assertEquals(
            SearchStopFieldType.FROM,
            pickFieldFor(FormShape.Single, ToFromField.To, SearchStopFieldType.FROM),
        )
    }

    @Test
    fun `the start fills itself with the nearest stop within a kilometre and never a far one`() {
        val rider = LatLng(-33.8734, 151.2069)
        val near = NearbyStopFeature("200070", "Town Hall Station", LatLng(-33.8734, 151.2069), persistentListOf())
        val far = NearbyStopFeature("215020", "Parramatta Station", LatLng(-33.8176, 151.0054), persistentListOf())

        assertEquals(
            StopItem(stopId = "200070", stopName = "Town Hall Station"),
            autoFromStop(rider, listOf(far, near)),
        )
        assertNull(autoFromStop(rider, listOf(far)))
        assertNull(autoFromStop(rider, emptyList()))
    }

    // endregion

    // region the pill

    @Test
    fun `the pill is hidden once location is on, however the lookup read`() {
        assertEquals(LocationPill.Hidden, locationPillFor(PermissionStatus.Granted, hasUserLocation = false))
        assertEquals(LocationPill.Hidden, locationPillFor(PermissionStatus.Denied(false), hasUserLocation = true))
        assertEquals(LocationPill.Hidden, locationPillFor(PermissionStatus.NotDetermined, hasUserLocation = true))
    }

    @Test
    fun `the pill never flashes in before the permission has been looked up`() {
        assertEquals(LocationPill.Hidden, locationPillFor(status = null, hasUserLocation = false))
    }

    @Test
    fun `a rider who has not been asked is asked`() {
        assertEquals(LocationPill.Ask, locationPillFor(PermissionStatus.NotDetermined, hasUserLocation = false))
    }

    @Test
    fun `a rider who refused, or cannot be asked, is sent to Settings`() {
        assertEquals(LocationPill.OpenSettings, locationPillFor(PermissionStatus.Denied(false), false))
        assertEquals(LocationPill.OpenSettings, locationPillFor(PermissionStatus.Denied(true), false))
        assertEquals(LocationPill.OpenSettings, locationPillFor(PermissionStatus.Restricted, false))
    }

    // endregion

    @Test
    fun `the location is granted only by a granted permission or a fix in hand, never by not knowing yet`() {
        assertEquals(true, locationGrantedFor(PermissionStatus.Granted, hasUserLocation = false))
        assertEquals(true, locationGrantedFor(null, hasUserLocation = true))
        assertEquals(false, locationGrantedFor(null, hasUserLocation = false))
        assertEquals(false, locationGrantedFor(PermissionStatus.NotDetermined, hasUserLocation = false))
        assertEquals(false, locationGrantedFor(PermissionStatus.Denied(canAskAgain = false), hasUserLocation = false))
        assertEquals(false, locationGrantedFor(PermissionStatus.Denied(canAskAgain = true), hasUserLocation = false))
    }
}
