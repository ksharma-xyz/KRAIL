package xyz.ksharma.krail.discover.network.api

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.discover.network.api.DiscoverModel.Button.Social.PartnerSocial.PartnerSocialType
import xyz.ksharma.krail.social.network.api.model.SocialType

val previewDiscoverCardList = listOf(
    DiscoverModel(
        title = "Cta only card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1749751234397-41ee8a7887c2"),
        type = DiscoverModel.DiscoverCardType.Travel,
        buttons = persistentListOf(
            DiscoverModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
        ),
    ),
    DiscoverModel(
        title = "No Buttons Card Title",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1741851373856-67a519f0c014"),
        type = DiscoverModel.DiscoverCardType.Events,
        buttons = persistentListOf(),
    ),
    DiscoverModel(
        title = "App Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        type = DiscoverModel.DiscoverCardType.Krail,
        buttons = persistentListOf(DiscoverModel.Button.Social.AppSocial)
    ),
    DiscoverModel(
        title = "Partner Social Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752624906994-d94727d34c9b"),
        type = DiscoverModel.DiscoverCardType.Events,
        buttons = persistentListOf(
            DiscoverModel.Button.Social.PartnerSocial(
                socialPartnerName = "XYZ Place",
                links = persistentListOf(
                    PartnerSocialType(type = SocialType.Facebook, url = "https://example.com"),
                )
            )
        )
    ),
    DiscoverModel(
        title = "Share Only Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1751906599846-2e31345c8014"),
        type = DiscoverModel.DiscoverCardType.Kids,
        buttons = persistentListOf(
            DiscoverModel.Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverModel(
        title = "Cta + Share Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1730005745671-dbbfddfe387e"),
        type = DiscoverModel.DiscoverCardType.Sports,
        buttons = persistentListOf(
            DiscoverModel.Button.Cta(
                label = "Click Me",
                url = "https://example.com/cta",
            ),
            DiscoverModel.Button.Share(
                shareUrl = "https://example.com/share",
            ),
        )
    ),
    DiscoverModel(
        title = "Feedback Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://plus.unsplash.com/premium_photo-1752832756659-4dd7c40f5ae7"),
        type = DiscoverModel.DiscoverCardType.Events,
        buttons = persistentListOf(
            DiscoverModel.Button.Feedback(
                label = "Feedback",
                url = "https://example.com/feedback",
            ),
        )
    ),
    DiscoverModel(
        title = "Credits Disclaimer Card",
        description = "This is a sample description for the Discover Card. It can be used to display additional information.",
        imageList = persistentListOf("https://images.unsplash.com/photo-1735746693939-586a9d7558e1"),
        type = DiscoverModel.DiscoverCardType.Events,
        disclaimer = "Image credits: Unsplash",
        buttons = persistentListOf(
            DiscoverModel.Button.Cta(
                label = "Cta Button",
                url = "https://example.com/feedback",
            ),
        )
    ),
)
