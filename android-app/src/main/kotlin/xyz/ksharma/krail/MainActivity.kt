package xyz.ksharma.krail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import xyz.ksharma.krail.common.KrailApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KrailApp()
        }
    }

    /*
        private val koinConfig1 = koinConfiguration {
            androidContext(androidContext = this@MainActivity.applicationContext)
            modules(androidDbModule)
            includes(koinConfig)
        }
    */
}
