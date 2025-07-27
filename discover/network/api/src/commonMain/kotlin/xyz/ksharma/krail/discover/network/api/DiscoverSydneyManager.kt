package xyz.ksharma.krail.discover.network.api

interface DiscoverSydneyManager {

    /**
     * Returns a list of cards to be displayed in the Discover screen.
     */
    suspend fun fetchDiscoverData(): List<DiscoverModel>

}
