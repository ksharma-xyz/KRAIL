package xyz.ksharma.krail

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge and allow status bar content color changes
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KrailApp()
        }
    }
}
