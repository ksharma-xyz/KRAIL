package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
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
    fun `a single bar names what it is for`() {
        assertEquals("Choose a stop", barPlaceholderFor(ToFromField.To, SearchStopFieldType.LABEL, false))
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

    // endregion

    // region the start sent with a destination

    @Test
    fun `a destination pick sends the auto filled start with it`() {
        assertEquals(
            central,
            startToSendWith(townHall, ToFromField.To, FormShape.ToAndFrom, autoFrom = central),
        )
    }

    @Test
    fun `no start is sent without an auto filled one, or when the start is being picked`() {
        assertNull(startToSendWith(townHall, ToFromField.To, FormShape.ToAndFrom, autoFrom = null))
        assertNull(startToSendWith(townHall, ToFromField.From, FormShape.ToAndFrom, autoFrom = central))
    }

    @Test
    fun `no start is sent for a label or a single leg`() {
        assertNull(startToSendWith(townHall, ToFromField.To, FormShape.Single, autoFrom = central))
    }

    @Test
    fun `a destination that is the same stop as the auto filled start sends nothing`() {
        assertNull(startToSendWith(central, ToFromField.To, FormShape.ToAndFrom, autoFrom = central))
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
}
