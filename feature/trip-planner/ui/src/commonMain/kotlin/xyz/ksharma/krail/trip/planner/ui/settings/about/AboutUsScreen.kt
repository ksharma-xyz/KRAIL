package xyz.ksharma.krail.trip.planner.ui.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.AppLogo

@Composable
fun AboutUsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Our story") },
            )
        }

        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp),
        ) {
            item {
                Text(
                    "Welcome to KRAIL, and thank you so much for using the app 🫶. " +
                            "I truly hope it’s made getting around Sydney just a little easier 🛤️.\n\n" +
                            "Every detail in this app, from the colors and buttons to the animations, " +
                            "was crafted with care, passion ❤️, and many late nights and weekends 🌙 " +
                            "envisioning the digital experience for you. KRAIL isn’t built by a " +
                            "company or a team. It is built by one person, simply trying to create " +
                            "something calm, helpful, and free from distraction 🧘.\n\n" +
                            "I’m Karan. I live in Sydney 🏙️, and I originally built KRAIL for myself — " +
                            "just to check the next train without scrolling past ads. I also needed " +
                            "the text to be larger than usual to read comfortably, something most " +
                            "popular apps don’t handle well 😿. So, I set out to build " +
                            "something more accessible and fun 🎨, an app that could support different " +
                            "needs while staying simple, clear, and easy to use ✅.\n\n" +
                            "At first, it was just mine. Then I shared it with friends and family 👨‍👩‍👧‍👦, " +
                            "and they shared it with others. Slowly, it started to grow, not " +
                            "through ads or big launches, but through people who found it helpful " +
                            "and passed it along 🤝. Maybe that’s how it reached you, too \uD83D\uDC9E.\n\n" +
                            "If KRAIL has helped you in any way, I’d really love to hear from you 💬. " +
                            "Many features were added thanks to someone taking a moment to share an idea 💡. " +
                            "Whether it’s a suggestion, a bug 🐛, or just a hello 👋, feel free to " +
                            "email me anytime at hey@krail.app. I read every single message ✉️, " +
                            "and your feedback means the world to me 🌏.\n\n" +
                            "And if you’ve found KRAIL helpful, I hope you’ll share it with someone " +
                            "else, just like someone once shared it with you 💞.\n\n" +
                            "Thanks again for being part of this journey 🚆.",
                    style = KrailTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }

            item {
                Text(
                    text = "Disclaimer:",
                    style = KrailTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 32.dp)
                )
            }
            item {
                Text(
                    "Real-time data in KRAIL is provided by Transport for NSW. " +
                            "I do my best to keep everything accurate, but I can’t guarantee it " +
                            "will always be correct. For the latest updates, visit www.transportnsw.info.\n",
                    style = KrailTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)
                )
            }

            item {
                AppLogo(modifier = Modifier.padding(top = 32.dp))
            }
        }
    }
}
