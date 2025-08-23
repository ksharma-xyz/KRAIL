package xyz.ksharma.krail.upgrade

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.Text

@Composable
fun ForceUpgradeScreen() {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Force Upgrade Required")
        // Button tp redirect to Play Store or App Store
    }
}
