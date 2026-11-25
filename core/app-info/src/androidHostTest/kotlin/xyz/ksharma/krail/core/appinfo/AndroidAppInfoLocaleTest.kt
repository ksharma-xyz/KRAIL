package xyz.ksharma.krail.core.appinfo

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AndroidAppInfoLocaleTest {

    private fun contextWithLanguages(vararg tags: String): Context {
        val base = RuntimeEnvironment.getApplication()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(*tags.map(Locale::forLanguageTag).toTypedArray()))
        return base.createConfigurationContext(configuration)
    }

    @Test
    fun `the locale is the first of the phone's languages and nothing after it`() {
        val context = contextWithLanguages("en-AU", "zh-Hans-CN", "hi-IN")

        assertEquals("en-AU", AndroidAppInfo(context).locale)
    }

    @Test
    fun `a phone with one language reports that language`() {
        val context = contextWithLanguages("fr-FR")

        assertEquals("fr-FR", AndroidAppInfo(context).locale)
    }

    @Test
    fun `a script subtag is kept`() {
        val context = contextWithLanguages("zh-Hant-TW", "en-AU")

        assertEquals("zh-Hant-TW", AndroidAppInfo(context).locale)
    }
}
