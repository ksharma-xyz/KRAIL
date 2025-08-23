package xyz.ksharma.krail.upgrade

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor

@Composable
fun ForceUpgradeScreen(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Rotating animation for settings gear
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Pounding animation for hammer
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    // More realistic pounding animation - mimics actual hammering motion
    val hammerRotation by infiniteTransition.animateFloat(
        initialValue = -45f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val hammerScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Text(
                text = "KRAIL",
                style = KrailTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                ),
                color = themeColor(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )

            // Choose one of these animated icons:

            // Option 1: Rotating Settings Gear
          /*  Text(
                text = "⚙️",
                style = KrailTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .rotate(rotation)
            )*/

            // Option 2: Pounding Hammer with rotation and scale
            Text(
                text = "🔨",
                style = KrailTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .rotate(hammerRotation)
                    .scale(hammerScale)
            )

        }

        Column(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "Update Required",
                style = KrailTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please update KRAIL to continue using the\u00A0app.",
                style = KrailTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {},
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .padding(vertical = 20.dp),
        ) {
            Text("Update Now")
        }
    }
}

@Preview
@Composable
fun ForceUpgradeScreenPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ForceUpgradeScreen()
    }
}