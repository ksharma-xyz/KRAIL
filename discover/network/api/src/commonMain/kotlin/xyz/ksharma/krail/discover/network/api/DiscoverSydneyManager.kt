package xyz.ksharma.krail.discover.network.api

interface DiscoverSydneyManager {

    /**
     * Returns a list of cards to be displayed in the Discover screen.
     */
    fun fetchDiscoverData(): List<DiscoverModel>

}
