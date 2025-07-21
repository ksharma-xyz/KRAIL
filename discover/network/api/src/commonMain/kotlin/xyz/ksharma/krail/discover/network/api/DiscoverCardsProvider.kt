package xyz.ksharma.krail.discover.network.api

interface DiscoverCardsProvider {

    /**
     * Returns a list of cards to be displayed in the Discover screen.
     */
    fun getCards(): List<DiscoverCard>

}
