package xyz.ksharma.krail.taj.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.krail.taj.resources.Res
import app.krail.taj.resources.ic_android_share
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun rememberShareIconPainter(): Painter = painterResource(Res.drawable.ic_android_share)
