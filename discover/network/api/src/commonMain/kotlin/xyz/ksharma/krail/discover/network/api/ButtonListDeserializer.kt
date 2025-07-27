package xyz.ksharma.krail.discover.network.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.social.network.api.model.SocialType

object ButtonListSerializer : KSerializer<List<DiscoverModel.Button>> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("ButtonList", StructureKind.LIST)

    override fun deserialize(decoder: Decoder): List<DiscoverModel.Button> {
        val input = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonArray = input.decodeJsonElement().jsonArray

        return jsonArray.mapNotNull { buttonElement ->
            val buttonObj = buttonElement.jsonObject

            when (buttonObj["buttonType"]?.jsonPrimitive?.content) {
                "Cta" -> {
                    val label = buttonObj["label"]?.jsonPrimitive?.content
                    val url = buttonObj["url"]?.jsonPrimitive?.content
                    require(!label.isNullOrBlank()) { "Button Cta label cannot be null or blank" }
                    require(!url.isNullOrBlank()) { "Button Cta URL cannot be null or blank" }

                    DiscoverModel.Button.Cta(
                        label = label,
                        url = url,
                    )
                }

                "Share" -> {
                    val shareUrl = buttonObj["shareUrl"]?.jsonPrimitive?.content
                    require(!shareUrl.isNullOrBlank()) { "Button Share URL cannot be null or blank" }

                    DiscoverModel.Button.Share(
                        shareUrl = shareUrl,
                    )
                }

                "Feedback" -> {
                    val label = buttonObj["label"]?.jsonPrimitive?.content
                    val url = buttonObj["url"]?.jsonPrimitive?.content
                    require(!label.isNullOrBlank()) { "Button Feedback label cannot be null or blank" }
                    require(!url.isNullOrBlank()) { "Button Feedback URL cannot be null or blank" }

                    DiscoverModel.Button.Feedback(
                        label = label,
                        url = url,
                    )
                }

                "AppSocial" -> DiscoverModel.Button.Social.AppSocial

                "PartnerSocial" -> {
                    val partnerName = buttonObj["socialPartnerName"]?.jsonPrimitive?.content
                    require(!partnerName.isNullOrBlank()) { "Button PartnerSocial socialPartnerName cannot be null or blank" }
                    val linksA = buttonObj["links"]?.jsonArray
                    require(!linksA.isNullOrEmpty()) { "Button PartnerSocial links cannot be null or empty" }

                    val links = linksA.mapNotNull { linkElement ->
                        val linkObj = linkElement.jsonObject
                        val typeStr =
                            linkObj["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val type = SocialType.valueOf(typeStr)

                        val url = linkObj["url"]?.jsonPrimitive?.content
                        require(!url.isNullOrBlank()) { "Button PartnerSocial link URL cannot be null or blank" }

                        DiscoverModel.Button.Social.PartnerSocial.PartnerSocialType(type, url)
                    }
                    DiscoverModel.Button.Social.PartnerSocial(
                        socialPartnerName = partnerName,
                        links = links
                    )
                }

                else -> {
                    logError("Unknown button type: ${buttonObj["buttonType"]?.jsonPrimitive?.content}")
                    null
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<DiscoverModel.Button>) {
        error("Serialization not supported")
    }
}
