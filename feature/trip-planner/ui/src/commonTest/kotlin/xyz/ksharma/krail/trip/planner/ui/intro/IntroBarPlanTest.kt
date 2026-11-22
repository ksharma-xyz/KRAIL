package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntroBarPlanTest {

    private class Row(
        val name: String,
        val left: Int,
        val right: Int,
        val available: Int,
        val plan: IntroBarPlan,
    )

    // Pixels, with a gap of 16. Each row says what the two buttons need to show their whole label on one line.
    private val table = listOf(
        Row("both under half, an even split", 120, 180, 400, IntroBarPlan.OneRow(192, 192)),
        Row("the longer needs exactly half", 100, 192, 400, IntroBarPlan.OneRow(192, 192)),
        Row("over half but both fit, spare shared", 100, 250, 400, IntroBarPlan.OneRow(117, 267)),
        Row("both fit with nothing to spare", 150, 234, 400, IntroBarPlan.OneRow(150, 234)),
        Row("one pixel too wide for a row", 150, 235, 400, IntroBarPlan.Stacked),
        Row("two long labels stack", 250, 300, 360, IntroBarPlan.Stacked),
        Row("a room of nothing stacks", 10, 10, 0, IntroBarPlan.Stacked),
    )

    @Test
    fun `the plan is the one the table says`() {
        table.forEach { row ->
            assertEquals(row.plan, introBarPlan(row.left, row.right, GAP, row.available), row.name)
        }
    }

    @Test
    fun `in a row the two buttons and the gap fill the room exactly and neither gets less than it needs`() {
        (0..MAX_NEED step LEFT_STEP).forEach { left ->
            (0..MAX_NEED step RIGHT_STEP).forEach { right ->
                val plan = introBarPlan(left, right, GAP, ROOM)

                if (plan is IntroBarPlan.OneRow) {
                    assertEquals(ROOM, plan.leftWidth + GAP + plan.rightWidth, "$left $right")
                    assertTrue(plan.leftWidth >= left && plan.rightWidth >= right, "$left $right gave $plan")
                } else {
                    val fits = left + right + GAP <= ROOM
                    assertTrue(!fits, "$left $right fit side by side but stacked")
                }
            }
        }
    }

    private companion object {
        const val GAP = 16
        const val ROOM = 480
        const val MAX_NEED = 600
        const val LEFT_STEP = 7
        const val RIGHT_STEP = 11
    }
}
