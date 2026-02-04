package xyz.ksharma.krail.taj.preview

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation that shows all theme and font scale combinations
 */
@Preview(
    name = "1. Light Mode",
    group = "Screen",
    showBackground = true,
    device = Devices.PHONE,
)
@Preview(
    name = "2. Dark Mode",
    group = "Screen",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
    device = Devices.PHONE,
    showBackground = true,
)
@Preview(
    name = "3. 2x",
    group = "Screen",
    fontScale = 2.0f,
    showBackground = true,
    device = Devices.PHONE,
)
@Preview(
    name = "4. Large Screen",
    group = "Screen",
    fontScale = 1.0f,
    showBackground = true,
    device = Devices.TABLET,
)
annotation class ScreenPreview

/**
 * Comprehensive preview with all combinations
 */
@Preview(
    name = "Light Mode",
    group = "Screen",
    showBackground = true,
    device = Devices.PHONE,
)
@Preview(
    name = "Dark Mode",
    group = "Screen",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
    device = Devices.PHONE,
    showBackground = true,
)
@Preview(
    name = "2x",
    group = "Screen",
    fontScale = 2.0f,
    showBackground = true,
    device = Devices.PHONE,
)
annotation class ComponentPreview
