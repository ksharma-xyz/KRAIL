package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.yield
import xyz.ksharma.krail.feature.crowding.CarriageForecastRepository
import xyz.ksharma.krail.feature.crowding.CarriageIndex

/**
 * Canonical fake for [CarriageForecastRepository]. Set [result] to drive success or failure;
 * [loadCount] proves how many downloads a screen asked for.
 */
class FakeCarriageForecastRepository(
    var result: Result<CarriageIndex> = Result.success(CarriageIndex.EMPTY),
) : CarriageForecastRepository {

    var loadCount = 0
        private set

    override suspend fun load(): Result<CarriageIndex> {
        loadCount++
        yield()
        return result
    }
}
