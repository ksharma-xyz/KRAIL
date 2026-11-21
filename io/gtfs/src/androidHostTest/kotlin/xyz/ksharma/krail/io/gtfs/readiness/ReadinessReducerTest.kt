package xyz.ksharma.krail.io.gtfs.readiness

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadinessReducerTest {

    private fun done(
        storedVersion: Long = EXPECTED,
        stopCount: Int = FLOOR,
        canaryFound: Boolean = true,
    ) = ReadinessInput.InsertDone(
        storedVersion = storedVersion,
        expectedVersion = EXPECTED,
        stopCount = stopCount,
        minimumStopCount = FLOOR,
        canaryFound = canaryFound,
    )

    private class Case(val name: String, val input: ReadinessInput, val expected: StopsReadiness)

    @Test
    fun `every combination of evidence maps to exactly one readiness`() {
        val cases = listOf(
            Case("nothing has run", ReadinessInput.NotStarted, StopsReadiness.Unknown),
            Case("an insert is running", ReadinessInput.Running, StopsReadiness.Loading),
            Case("the insert failed", ReadinessInput.InsertFailed("disk"), StopsReadiness.Failed(ATTEMPT, "disk")),
            Case("all good", done(stopCount = FLOOR + 5), StopsReadiness.Ready(FLOOR + 5)),
            Case("count exactly at the floor", done(stopCount = FLOOR), StopsReadiness.Ready(FLOOR)),
            Case(
                "stored version newer than bundled, as the insert guard treats it",
                done(storedVersion = EXPECTED + 1),
                StopsReadiness.Ready(FLOOR),
            ),
            Case(
                "stored version old, rows and canary look fine",
                done(storedVersion = EXPECTED - 1),
                StopsReadiness.Failed(ATTEMPT, REASON_VERSION_OLD),
            ),
            Case(
                "no stored version at all",
                done(storedVersion = 0L),
                StopsReadiness.Failed(ATTEMPT, REASON_VERSION_OLD),
            ),
            Case(
                "count one under the floor",
                done(stopCount = FLOOR - 1),
                StopsReadiness.Failed(ATTEMPT, REASON_COUNT_LOW),
            ),
            Case(
                "canary missing, version and count fine",
                done(canaryFound = false),
                StopsReadiness.Failed(ATTEMPT, REASON_CANARY_MISSING),
            ),
            Case(
                "version old and canary missing reports the version first",
                done(storedVersion = EXPECTED - 1, canaryFound = false),
                StopsReadiness.Failed(ATTEMPT, REASON_VERSION_OLD),
            ),
        )

        cases.forEach { case ->
            assertEquals(case.expected, reduceReadiness(case.input, ATTEMPT), case.name)
        }
    }

    private companion object {
        const val EXPECTED = 91L
        const val FLOOR = 36_000
        const val ATTEMPT = 2
    }
}
