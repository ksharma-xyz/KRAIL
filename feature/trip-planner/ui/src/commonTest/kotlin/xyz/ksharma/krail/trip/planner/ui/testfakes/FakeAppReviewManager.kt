package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.DelightMoment

/**
 * Records calls instead of touching the platform review APIs. Eligibility itself is
 * covered by `RealAppReviewManagerTest` in `:core:app-review`; ViewModel tests only care
 * that the right moments are armed and that the screen fires the check.
 */
class FakeAppReviewManager : AppReviewManager {

    val armedMoments = mutableListOf<DelightMoment>()

    var savedTripsScreenShownCount = 0
        private set

    override fun onDelightMoment(moment: DelightMoment) {
        armedMoments += moment
    }

    override fun onSavedTripsScreenShown() {
        savedTripsScreenShownCount++
    }
}
