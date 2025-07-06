package xyz.ksharma.krail.core.festival.model

import kotlinx.serialization.Serializable

@Serializable
data class Festival(
    /**
     * Festival start date in ISO 8601 format (YYYY-MM-DD).
     */
    val startDate: String,

    /**
     * Festival end date in ISO 8601 format (YYYY-MM-DD).
     */
    val endDate: String,

    /**
     * Emoji list representing the festival.
     */
    val emojiList: String,

    /**
     * Description of the festival.
     */
    val description: String
)
