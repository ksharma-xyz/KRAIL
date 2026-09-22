package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The row an attempt produces, and in particular the parts of it that depend on something being
 * supplied from outside.
 *
 * `hadLabelWord` shipped in a first draft reporting `false` on every row, including sentences
 * that plainly contained a label word, because the labels it reads were never wired in the DI
 * module and defaulted to an empty list. Nothing failed: the param arrived, looked healthy, and
 * meant nothing. That is the same shape as a param Firebase silently rejects, and the reason
 * `AnalyticsEvent.kt`'s `~trunc` suffix exists.
 *
 * These tests hold the reporter's half. The wiring itself lives in `ViewModelModule` and is not
 * reachable from here, so a dead supplier would still pass: see the PR notes rather than
 * assuming this covers it.
 */
class AiAttemptReporterTest {

    @Test
    fun `a label word is reported when the rider has labels`() = runTest {
        val analytics = FakeAnalytics()
        val reporter = AiAttemptReporter(
            analytics = analytics,
            riderLabels = { listOf("Work") },
            newAskSessionId = { "session" },
        )
        reporter.beginAttempt()
        reporter.labelsForThisAttempt()

        reporter.report(
            state = AiSearchInputUiState(typedText = "get me to work by 9"),
            reason = "none",
            riderText = "get me to work by 9",
            extraction = TripIntentExtraction(destinationText = "work"),
            extractMs = 10,
        )

        assertTrue(analytics.attempt().hadLabelWord)
    }

    @Test
    fun `no labels means no label word, not an error`() = runTest {
        val analytics = FakeAnalytics()
        val reporter = AiAttemptReporter(
            analytics = analytics,
            riderLabels = { emptyList() },
            newAskSessionId = { "session" },
        )
        reporter.beginAttempt()
        reporter.labelsForThisAttempt()

        reporter.report(
            state = AiSearchInputUiState(typedText = "get me to work by 9"),
            reason = "none",
            riderText = "get me to work by 9",
            extraction = TripIntentExtraction(destinationText = "work"),
            extractMs = 10,
        )

        assertEquals(false, analytics.attempt().hadLabelWord)
    }

    @Test
    fun `labels are read once per attempt, not once per reporter`() = runTest {
        // A rider who sets a label mid-session must not keep reporting the old set. Reading on
        // every attempt also means the DI supplier is exercised repeatedly rather than once at
        // construction, where a failure would be invisible after startup.
        var reads = 0
        val reporter = AiAttemptReporter(
            analytics = FakeAnalytics(),
            riderLabels = {
                reads += 1
                listOf("Work")
            },
            newAskSessionId = { "session" },
        )

        reporter.labelsForThisAttempt()
        reporter.labelsForThisAttempt()

        assertEquals(2, reads)
    }

    @Test
    fun `one dialog session is one id and one attempt sequence`() = runTest {
        val analytics = FakeAnalytics()
        var n = 0
        val reporter = AiAttemptReporter(
            analytics = analytics,
            riderLabels = { emptyList() },
            newAskSessionId = { "session-${n++}" },
        )

        reporter.beginAttempt()
        reporter.reportBlank(analytics)
        reporter.beginAttempt()
        reporter.reportBlank(analytics)

        reporter.startSession()
        reporter.beginAttempt()
        reporter.reportBlank(analytics)

        val rows = analytics.attempts()
        assertEquals(listOf(1, 2, 1), rows.map { it.attemptIndex })
        assertEquals(rows[0].askSessionId, rows[1].askSessionId)
        assertTrue(rows[2].askSessionId != rows[1].askSessionId)
    }

    private suspend fun AiAttemptReporter.reportBlank(analytics: FakeAnalytics) {
        labelsForThisAttempt()
        report(
            state = AiSearchInputUiState(),
            reason = "none",
            riderText = "",
            extraction = null,
            extractMs = null,
        )
        check(analytics.attempts().isNotEmpty())
    }

    private fun FakeAnalytics.attempt(): AnalyticsEvent.AskKrailAttemptEvent = attempts().first()

    private fun FakeAnalytics.attempts(): List<AnalyticsEvent.AskKrailAttemptEvent> =
        getTrackedEvents("ask_krail_attempt")
            .filterIsInstance<AnalyticsEvent.AskKrailAttemptEvent>()
}
