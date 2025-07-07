package xyz.ksharma.core.test.fakes

import kotlinx.datetime.LocalDate
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.Festival

class FakeFestivalManager : FestivalManager {
    var festivalForDate: MutableMap<LocalDate, Festival?> = mutableMapOf()
    var emojiForDate: MutableMap<LocalDate, String> = mutableMapOf()

    override fun festivalOnDate(date: LocalDate): Festival? {
        return festivalForDate[date]
    }

    override fun emojiForDate(date: LocalDate): String {
        return emojiForDate[date] ?: "😀"
    }
}
