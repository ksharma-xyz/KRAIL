package xyz.ksharma.krail

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.exposeTestTagsToUiAutomation(): Modifier =
    semantics { testTagsAsResourceId = true }
