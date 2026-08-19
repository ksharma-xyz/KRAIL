package xyz.ksharma.krail.taj

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.barbie_pink_theme
import xyz.ksharma.krail.taj.theme.bus_theme
import xyz.ksharma.krail.taj.theme.coach_theme
import xyz.ksharma.krail.taj.theme.ferry_theme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.theme.light_rail_theme
import xyz.ksharma.krail.taj.theme.metro_theme
import xyz.ksharma.krail.taj.theme.train_theme
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorUtilsTest {

    @Test
    fun testGetForegroundColor_withForegroundColor() {
        val backgroundColor = Color(0xFF000000)
        val foregroundColor = Color(0xFFFFFFFF)
        assertEquals(foregroundColor, getForegroundColor(backgroundColor, foregroundColor))
    }

    @Test
    fun testGetForegroundColor_withoutForegroundColor() {
        val backgroundColor = Color(0xFF000000)
        val expectedColor = Color(0xFFFCF6F1) // md_theme_dark_onSurface
        assertEquals(expectedColor, getForegroundColor(backgroundColor))
    }

    @Test
    fun testColorContrastKrailThemeStyle() {
        assertEquals("#FFFCF6F1", getForegroundColor(light_rail_theme).toHex())
        assertEquals("#FF010101", getForegroundColor(train_theme).toHex())
        assertEquals("#FF010101", getForegroundColor(bus_theme).toHex())
        assertEquals("#FF010101", getForegroundColor(metro_theme).toHex())
        assertEquals("#FF010101", getForegroundColor(barbie_pink_theme).toHex())
        assertEquals("#FF010101", getForegroundColor(ferry_theme).toHex())
        assertEquals("#FFFCF6F1", getForegroundColor(coach_theme).toHex())
    }
}
