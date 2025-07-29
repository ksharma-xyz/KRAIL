package xyz.ksharma.krail.discover.network.api.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.ksharma.krail.discover.network.api.serializer.ButtonListSerializer
import xyz.ksharma.krail.discover.network.api.serializer.ImagesListSerializer
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType

@Serializable
@Stable
data class DiscoverModel(

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String,

    // ISO 8601 date format
    @SerialName("startDate")
    val startDate: String? = null,

    // ISO 8601 date format
    @SerialName("endDate")
    val endDate: String? = null,

    // image credits etc.
    @SerialName("disclaimer")
    val disclaimer: String? = null,

    /**
     * List of image URLs to be displayed in the card.
     */
    @Serializable(with = ImagesListSerializer::class)
    val imageList: List<String>,

    @Serializable(with = ButtonListSerializer::class)
    val buttons: List<Button>? = null,

    @SerialName("type")
    val type: DiscoverCardType,

    @SerialName("cardId")
    val cardId: String,
)
