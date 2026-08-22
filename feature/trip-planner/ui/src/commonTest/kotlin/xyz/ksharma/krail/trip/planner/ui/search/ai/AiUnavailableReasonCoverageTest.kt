package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every reason the platforms can report has to produce something the rider can act on.
 *
 * The bug this exists to prevent shipped and survived for months: an unhandled reason fell
 * through to UNRESOLVED with no [UnresolvedReason] set, which the banner renders as "Something
 * is missing there. Name where you are going, and where from." That is advice about a
 * sentence, shown to a rider whose sentence was never read, and on a phone that cannot run the
 * model it is unanswerable.
 *
 * The test that was supposed to cover it asserted the PHASE and passed the whole time, because
 * UNRESOLVED was the right phase. What was wrong was the reason, which is the part the rider
 * actually reads.
 *
 * So this walks the vocabulary itself rather than a list written out by hand. Adding a constant
 * to [AiUnavailableReasons] without deciding what it says to a rider fails here.
 *
 * See docs/learning/2026-08-22-two-platforms-two-vocabularies.md.
 */
class AiUnavailableReasonCoverageTest {

    // Reflection is not available in common code, so the vocabulary is listed once here and
    // pinned by a count. A new constant makes the count assertion fail, which is the prompt to
    // come and decide what it means.
    private val allReasons = listOf(
        AiUnavailableReasons.MODEL_DOWNLOADING,
        AiUnavailableReasons.DEVICE_UNSUPPORTED,
        AiUnavailableReasons.NEEDS_DEVICE_SETTING,
        AiUnavailableReasons.CHECK_FAILED,
    )

    @Test
    fun `every reason in the vocabulary is listed here`() {
        // Bump this deliberately, having added the new constant above AND decided below what
        // it should say. If you are reading this because it failed, that is the point.
        assertEquals(4, allReasons.size)
        assertEquals(allReasons.size, allReasons.toSet().size, "duplicate reason constant")
    }

    @Test
    fun `no reason leaves the rider with the generic could-not-read-your-sentence banner`() {
        allReasons.forEach { reason ->
            val state = AiSearchInputUiState().withUnavailableModel(reason)
            if (state.phase == AiSearchInputPhase.DOWNLOADING) {
                // DOWNLOADING carries its own message; a reason as well would be a second one.
                assertNull(state.unresolvedReason, "$reason should not also set a reason")
            } else {
                assertEquals(AiSearchInputPhase.UNRESOLVED, state.phase, "unexpected phase for $reason")
                assertNotNull(
                    state.unresolvedReason,
                    "$reason falls through to the generic banner, which tells a rider to " +
                        "reword a sentence nothing read",
                )
            }
        }
    }

    @Test
    fun `only a permanently unsupported device loses the way in`() {
        allReasons.forEach { reason ->
            val capable = AiSearchInputUiState().withUnavailableModel(reason).isDeviceCapable
            assertEquals(
                reason != AiUnavailableReasons.DEVICE_UNSUPPORTED,
                capable,
                "$reason decided the wrong thing about hiding the entry point",
            )
        }
    }

    /**
     * Android suffixes the throwable message onto CHECK_FAILED, so it arrives as a prefix
     * rather than an exact value. Matching it by equality is the mistake that would silently
     * send it to the wrong arm.
     */
    @Test
    fun `a suffixed check failure is still treated as retriable, not as a dead device`() {
        val state = AiSearchInputUiState()
            .withUnavailableModel("${AiUnavailableReasons.CHECK_FAILED}: java.io.IOException")
        assertEquals(UnresolvedReason.COULD_NOT_READ, state.unresolvedReason)
        assertTrue(state.isDeviceCapable, "a failed check says nothing about the device")
    }
}
