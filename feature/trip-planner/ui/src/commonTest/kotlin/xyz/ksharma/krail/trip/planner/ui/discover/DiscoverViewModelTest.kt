package xyz.ksharma.krail.trip.planner.ui.discover

import app.cash.turbine.test
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appinfo.KRAIL_WEBSITE_URL
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfo
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.social.state.SocialType
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeDiscoverSydneyManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `DiscoverViewModel` owns the card list, the filter chip row and every analytics call the Discover
 * surface makes. None of it was covered.
 *
 * The fetch is triggered by the first subscriber (`onStart` on a `WhileSubscribed` state flow), so
 * every test collects `uiState` rather than reading `.value` — reading the value alone never starts
 * the fetch, which is exactly the bug this shape can hide.
 */
class DiscoverViewModelTest {

    private val discoverManager = FakeDiscoverSydneyManager()
    private val analytics = FakeAnalytics()
    private val platformOps = FakePlatformOps()
    private val appInfoProvider = FakeAppInfoProvider()

    @Test
    fun `GIVEN cards WHEN first collected THEN they are fetched and mapped into the ui list`() = krailRunTest {
        discoverManager.cards = listOf(card(cardId = "a", title = "Vivid", type = DiscoverCardType.Events))

        viewModel().uiState.test {
            assertTrue(awaitItem().discoverCardsList.isEmpty())
            runCurrent()

            val card = expectMostRecentItem().discoverCardsList.single()
            assertEquals("a", card.cardId)
            assertEquals("Vivid", card.title)
            assertEquals(DiscoverCardType.Events, card.type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN mixed card types WHEN collected THEN the chip row is deduplicated and sort-ordered`() =
        krailRunTest {
            discoverManager.cards = listOf(
                card(cardId = "a", type = DiscoverCardType.Sports),
                card(cardId = "b", type = DiscoverCardType.Travel),
                card(cardId = "c", type = DiscoverCardType.Sports),
                card(cardId = "d", type = DiscoverCardType.Events),
            )

            viewModel().uiState.test {
                awaitItem()
                runCurrent()

                val state = expectMostRecentItem()
                assertEquals(
                    listOf(DiscoverCardType.Travel, DiscoverCardType.Events, DiscoverCardType.Sports),
                    state.sortedDiscoverCardTypes,
                )
                assertEquals(DiscoverCardType.Unknown, state.selectedType)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a chip tap WHEN filtering THEN only that type is listed and the choice is tracked`() =
        krailRunTest {
            discoverManager.cards = listOf(
                card(cardId = "sport", type = DiscoverCardType.Sports),
                card(cardId = "food", type = DiscoverCardType.Food),
            )
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                expectMostRecentItem()

                viewModel.onEvent(DiscoverEvent.FilterChipClicked(DiscoverCardType.Food))
                runCurrent()

                val filtered = expectMostRecentItem()
                assertEquals(listOf("food"), filtered.discoverCardsList.map { it.cardId })
                assertEquals(DiscoverCardType.Food, filtered.selectedType)
                // The chip row still offers every type — filtering must not shrink the row it
                // lives in.
                assertEquals(
                    listOf(DiscoverCardType.Food, DiscoverCardType.Sports),
                    filtered.sortedDiscoverCardTypes,
                )
                cancelAndIgnoreRemainingEvents()
            }

            val event = assertIs<AnalyticsEvent.DiscoverFilterChipSelected>(
                analytics.getTrackedEvent("discover_filter_chip_selected"),
            )
            assertEquals(AnalyticsEvent.DiscoverCardClick.CardType.FOOD, event.cardType)
        }

    @Test
    fun `GIVEN an active filter WHEN the same chip is tapped again THEN the filter clears`() = krailRunTest {
        discoverManager.cards = listOf(
            card(cardId = "sport", type = DiscoverCardType.Sports),
            card(cardId = "food", type = DiscoverCardType.Food),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            runCurrent()
            expectMostRecentItem()

            viewModel.onEvent(DiscoverEvent.FilterChipClicked(DiscoverCardType.Food))
            runCurrent()
            expectMostRecentItem()

            viewModel.onEvent(DiscoverEvent.FilterChipClicked(DiscoverCardType.Food))
            runCurrent()

            val cleared = expectMostRecentItem()
            assertEquals(listOf("sport", "food"), cleared.discoverCardsList.map { it.cardId })
            assertEquals(DiscoverCardType.Unknown, cleared.selectedType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a CTA tap WHEN handled THEN the link opens and the click is attributed to the cta`() =
        krailRunTest {
            val viewModel = viewModel()

            viewModel.onEvent(
                DiscoverEvent.CtaButtonClicked(
                    url = "https://example.com/vivid",
                    cardId = "a",
                    cardType = DiscoverCardType.Events,
                ),
            )

            assertEquals("https://example.com/vivid", platformOps.lastOpenedUrl)
            val event = assertIs<AnalyticsEvent.DiscoverCardClick>(
                analytics.getTrackedEvent("discover_card_click"),
            )
            assertEquals(AnalyticsEvent.DiscoverCardClick.Source.CTA_CLICK, event.source)
            assertEquals(AnalyticsEvent.DiscoverCardClick.CardType.EVENTS, event.cardType)
            assertEquals("a", event.cardId)
            assertNull(event.partnerSocialLink)
        }

    @Test
    fun `GIVEN a partner social tap WHEN handled THEN the partner platform and url are attributed`() =
        krailRunTest {
            val viewModel = viewModel()

            viewModel.onEvent(
                DiscoverEvent.PartnerSocialLinkClicked(
                    partnerSocialLink = Button.Social.PartnerSocial.PartnerSocialLink(
                        type = SocialType.Instagram,
                        url = "https://instagram.com/partner",
                    ),
                    cardId = "a",
                    cardType = DiscoverCardType.Food,
                ),
            )

            assertEquals("https://instagram.com/partner", platformOps.lastOpenedUrl)
            val event = assertIs<AnalyticsEvent.DiscoverCardClick>(
                analytics.getTrackedEvent("discover_card_click"),
            )
            assertEquals(AnalyticsEvent.DiscoverCardClick.Source.PARTNER_SOCIAL_LINK, event.source)
            assertEquals(
                AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatformType.INSTAGRAM,
                event.partnerSocialLink?.type,
            )
            assertEquals("https://instagram.com/partner", event.partnerSocialLink?.url)
        }

    @Test
    fun `GIVEN a KRAIL social tap WHEN handled THEN it is sourced to the discover card`() = krailRunTest {
        val viewModel = viewModel()

        viewModel.onEvent(DiscoverEvent.AppSocialLinkClicked(KrailSocialType.Reddit))

        assertEquals(KrailSocialType.Reddit.url, platformOps.lastOpenedUrl)
        val event = assertIs<AnalyticsEvent.SocialConnectionLinkClickEvent>(
            analytics.getTrackedEvent("social_connection_link_click"),
        )
        assertEquals(
            AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatformType.REDDIT,
            event.socialPlatformType,
        )
        assertEquals(
            AnalyticsEvent.SocialConnectionLinkClickEvent.SocialConnectionSource.DISCOVER_CARD,
            event.source,
        )
    }

    @Test
    fun `GIVEN a share tap WHEN handled THEN the card text, its cta link and the KRAIL tag are shared`() =
        krailRunTest {
            discoverManager.cards = listOf(
                card(
                    cardId = "a",
                    title = "Vivid Sydney",
                    description = "Lights on the harbour",
                    type = DiscoverCardType.Events,
                    buttons = listOf(Button.Cta(label = "Book", url = "https://example.com/vivid")),
                ),
            )
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                expectMostRecentItem()

                viewModel.onEvent(
                    DiscoverEvent.ShareButtonClicked(cardId = "a", cardType = DiscoverCardType.Events),
                )
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                "Vivid Sydney\nLights on the harbour\nhttps://example.com/vivid\n\n" +
                    "See more events in KRAIL App\n#LetsKRAIL $KRAIL_WEBSITE_URL",
                platformOps.lastSharedText,
            )
            val event = assertIs<AnalyticsEvent.DiscoverCardClick>(
                analytics.getTrackedEvent("discover_card_click"),
            )
            assertEquals(AnalyticsEvent.DiscoverCardClick.Source.SHARE_CLICK, event.source)
        }

    @Test
    fun `GIVEN a card scrolls into view WHEN seen THEN it is persisted and counted for the session`() =
        krailRunTest {
            val viewModel = viewModel()

            viewModel.onEvent(DiscoverEvent.CardSeen(cardId = "a"))
            viewModel.onEvent(DiscoverEvent.CardSeen(cardId = "a"))
            viewModel.onEvent(DiscoverEvent.CardSeen(cardId = "b"))
            runCurrent()

            assertEquals(listOf("a", "a", "b"), discoverManager.seenCardIds)
            // The session count is distinct cards, not impressions.
            assertEquals(setOf("a", "b"), viewModel.analyticsSessionSeenCardIds)
        }

    @Test
    fun `GIVEN a release build WHEN reset seen cards is requested THEN nothing is reset`() = krailRunTest {
        appInfoProvider.mockAppInfo = FakeAppInfo(isDebug = false)
        val viewModel = viewModel()

        viewModel.onEvent(DiscoverEvent.ResetAllSeenCards)
        runCurrent()

        assertEquals(0, discoverManager.resetCallCount)
    }

    @Test
    fun `GIVEN a debug build WHEN reset seen cards is requested THEN the store is cleared`() = krailRunTest {
        appInfoProvider.mockAppInfo = FakeAppInfo(isDebug = true)
        val viewModel = viewModel()

        viewModel.onEvent(DiscoverEvent.ResetAllSeenCards)
        runCurrent()

        assertEquals(1, discoverManager.resetCallCount)
    }

    @Test
    fun `GIVEN a card with a cta and a partner social WHEN resolving its link THEN button order decides`() =
        krailRunTest {
            val partnerSocial = Button.Social.PartnerSocial(
                socialPartnerName = "Partner",
                links = listOf(
                    Button.Social.PartnerSocial.PartnerSocialLink(
                        type = SocialType.Instagram,
                        url = "https://instagram.com/partner",
                    ),
                ),
            )
            val cta = Button.Cta(label = "Book", url = "https://example.com/book")
            discoverManager.cards = listOf(
                card(cardId = "social-first", buttons = listOf(partnerSocial, cta, Button.Share)),
                card(cardId = "cta-first", buttons = listOf(cta, partnerSocial, Button.Share)),
            )

            viewModel().uiState.test {
                awaitItem()
                runCurrent()
                val state = expectMostRecentItem()

                // There is no cta-beats-social precedence: the shared link is whichever link
                // button the card lists first.
                assertEquals(
                    "https://instagram.com/partner",
                    state.getCtaOrPartnerSocialLinkForCard("social-first"),
                )
                assertEquals("https://example.com/book", state.getCtaOrPartnerSocialLinkForCard("cta-first"))
                assertNull(state.getCtaOrPartnerSocialLinkForCard("no-such-card"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a card with only a share button WHEN resolving its link THEN there is nothing to share`() =
        krailRunTest {
            discoverManager.cards = listOf(card(cardId = "a", buttons = listOf(Button.Share)))

            viewModel().uiState.test {
                awaitItem()
                runCurrent()

                assertNull(expectMostRecentItem().getCtaOrPartnerSocialLinkForCard("a"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun KrailTestScope.viewModel() = DiscoverViewModel(
        discoverSydneyManager = discoverManager,
        ioDispatcher = ioDispatcher,
        analytics = analytics,
        platformOps = platformOps,
        appInfoProvider = appInfoProvider,
        appCoroutineScope = this,
    )

    private fun card(
        cardId: String,
        title: String = "Title",
        description: String = "Description",
        type: DiscoverCardType = DiscoverCardType.Events,
        buttons: List<Button>? = null,
    ) = DiscoverModel(
        title = title,
        description = description,
        imageList = listOf("https://example.com/image.png"),
        buttons = buttons,
        type = type,
        cardId = cardId,
    )
}
