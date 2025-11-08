package xyz.ksharma.krail.taj.snapshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest

/**
 * Test previews to verify scanner works.
 * These are in androidUnitTest to test if scanner can find them.
 */

@ScreenshotTest
@Preview
@Composable
fun TestPreview() {
    Box(modifier = Modifier.size(100.dp)) {
        Text("Test OKAY")
    }
}
