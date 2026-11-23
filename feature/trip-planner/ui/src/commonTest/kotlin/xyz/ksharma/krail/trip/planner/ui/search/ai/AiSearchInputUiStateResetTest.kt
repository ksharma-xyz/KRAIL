package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What a fresh Ask KRAIL state keeps from the one before, and what it drops. */
class AiSearchInputUiStateResetTest {

    private val lived = AiSearchInputUiState(
        typedText = "Central to Town Hall",
        phase = AiSearchInputPhase.UNRESOLVED,
        isInputOpen = true,
        isListening = true,
        speechTranscript = "central",
        speechUnavailableReason = "no mic",
        unmatchedPlace = "Atlantis",
        isFeatureEnabled = true,
        isDeviceCapable = true,
        isSpeechAvailable = false,
        listenOnOpenPending = false,
    )

    @Test
    fun `opening drops everything the rider produced and starts listening when the feature is on`() {
        val opened = lived.opened(enabled = true, isDeviceCapable = true)
        assertEquals("", opened.typedText)
        assertEquals(AiSearchInputPhase.IDLE, opened.phase)
        assertEquals("", opened.speechTranscript)
        assertEquals(null, opened.unmatchedPlace)
        assertTrue(opened.isInputOpen)
        assertTrue(opened.listenOnOpenPending)
        assertFalse(opened.isListening)
    }

    @Test
    fun `opening keeps what the phone said about speech and takes the device fact it is given`() {
        val opened = lived.opened(enabled = true, isDeviceCapable = false)
        assertFalse(opened.isSpeechAvailable)
        assertFalse(opened.isDeviceCapable)
    }

    @Test
    fun `opening with the feature off opens nothing and listens for nothing`() {
        val opened = lived.opened(enabled = false, isDeviceCapable = true)
        assertFalse(opened.isInputOpen)
        assertFalse(opened.isFeatureEnabled)
        assertFalse(opened.listenOnOpenPending)
    }

    @Test
    fun `starting over drops the rider's work and keeps the way back in`() {
        val restarted = lived.restarted(isDeviceCapable = true)
        assertEquals("", restarted.typedText)
        assertEquals(AiSearchInputPhase.IDLE, restarted.phase)
        assertFalse(restarted.isInputOpen)
        assertFalse(restarted.isListening)
        assertTrue(restarted.isFeatureEnabled)
        assertTrue(restarted.isDeviceCapable)
        assertFalse(restarted.isSpeechAvailable)
    }
}
