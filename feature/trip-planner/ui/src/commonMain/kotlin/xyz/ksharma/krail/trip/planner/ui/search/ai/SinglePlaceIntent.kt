package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction

private val departureMarkers = listOf("from ", "leaving", "leave ", "depart", "starting at", "start at")

/**
 * Puts a lone place in the field the rider meant.
 *
 * "let's go home" came back with home as the *origin*, which filled the From field with the
 * rider's home station and left the destination empty: the exact opposite of what they asked
 * for. The prompt now says so on both platforms, but a prompt is advice, not a contract, and
 * this one mistake writes a wrong stop into a field the rider then has to notice.
 *
 * So the correction is also made here, where it is deterministic and testable. A single place
 * is a destination unless the wording is clearly about leaving somewhere. Saying where you are
 * going is the overwhelmingly common case; "from Central" is the rarer one, and it announces
 * itself in the words.
 *
 * Nothing is touched when both ends are present, or when neither is.
 */
internal fun TripIntentExtraction.withSinglePlaceInTheRightField(riderText: String): TripIntentExtraction {
    val origin = originText
    val isLonePlaceInOrigin = !origin.isNullOrBlank() && destinationText.isNullOrBlank()
    val saysLeaving = departureMarkers.any { riderText.contains(it, ignoreCase = true) }

    return if (isLonePlaceInOrigin && !saysLeaving) {
        copy(originText = null, destinationText = origin)
    } else {
        this
    }
}
