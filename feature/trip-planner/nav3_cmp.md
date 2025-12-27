Navigation 3 in Compose Multiplatform﻿
Edit page19 December 2025
Android's Navigation library has been upgraded to Navigation 3, introducing a redesigned approach to navigation that works with Compose and takes into account feedback to the previous version of the library. Starting with version 1.10, Compose Multiplatform supports adopting Navigation 3 in multiplatform projects for all supported platforms: Android, iOS, desktop, and web.

Key changes﻿
Navigation 3 is more than a new version of the library — in a lot of ways it's a new library entirely. To learn more about the philosophy behind this redesign, see the Android Developers blog post.

Key changes in Navigation 3 include:

User-owned back stack. Instead of operating a single library back stack, you create and manage a SnapshotStateList of states, which the UI observes directly.

Low-level building blocks. Thanks to closer integration with Compose, the library allows more flexibility in implementing your own navigation components and behavior.

Adaptive layout system. With adaptive design, you can display multiple destinations at the same time and seamlessly switch between layouts.

Learn more about general design of Navigation 3 in Android documentation.

Dependencies setup﻿
To try out the multiplatform implementation of Navigation 3, add the following dependency to your version catalog:

[versions]
multiplatform-nav3-ui = "1.0.0-alpha05"

[libraries]
jetbrains-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "multiplatform-nav3-ui" }
While Navigation 3 is released as two artifacts, navigation3:navigation3-ui and navigation3:navigation3-common, only navigation-ui needs a separate Compose Multiplatform implementation. A dependency on navigation3-common is added transitively.

For projects using the Material 3 Adaptive and ViewModel libraries, also add the following navigation support artifacts:

[versions]
compose-multiplatform-adaptive = "1.3.0-alpha02"
compose-multiplatform-lifecycle = "2.10.0-alpha05"

[libraries]
jetbrains-material3-adaptiveNavigation3 = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3", version.ref = "compose-multiplatform-adaptive" }
jetbrains-lifecycle-viewmodelNavigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "compose-multiplatform-lifecycle" }
Finally, you can try out the proof-of-concept library created by a JetBrains engineer. The library integrates the multiplatform Navigation 3 with browser history navigation on the web:

[versions]
compose-multiplatform-navigation3-browser = "0.2.0"

[libraries]
navigation3-browser = { module = "com.github.terrakok:navigation3-browser", version.ref = "compose-multiplatform-navigation3-browser" }
Browser history navigation is expected to be supported by the base multiplatform Navigation 3 library in version 1.1.0.

Multiplatform support﻿
Navigation 3 is closely aligned with Compose, allowing an Android navigation implementation to work in common Compose Multiplatform code with minimal changes. To support non-JVM platforms like web and iOS, the only thing you need is to implement polymorphic serialization for destination keys.

You can compare extensive examples of Android-only and multiplatform apps using Navigation 3 on GitHub:

original Android repository with Navigation 3 recipes

Compose Multiplatform project with most of the same recipes

Polymorphic serialization for destination keys﻿
On Android, Navigation 3 relies on reflection-based serialization, which is not available when you target non-JVM platforms like iOS. To take this into account, the library has two overloads for the rememberNavBackStack() function:

The first overload only takes a set of NavKey references and requires a reflection-based serializer.

The second overload also takes a SavedStateConfiguration parameter that allows you to provide a SerializersModule and handle open polymorphism correctly across all platforms.

In the Navigation 3 multiplatform examples, polymorphic serialization can look like this:

@Serializable
private data object RouteA : NavKey

@Serializable
private data class RouteB(val id: String) : NavKey

// Creates the required serializing configuration for open polymorphism
private val config = SavedStateConfiguration {
serializersModule = SerializersModule {
polymorphic(NavKey::class) {
subclass(RouteA::class, RouteA.serializer())
subclass(RouteB::class, RouteB.serializer())
}
}
}

@Composable
fun BasicDslActivity() {
// Consumes the serializing configuration
val backStack = rememberNavBackStack(config, RouteA)

    NavDisplay(
        backStack = backStack,
        //...
    )
}
What's next﻿
Navigation 3 is covered in-depth on the Android Developer portal. While some of the documentation uses Android-specific examples, the core concepts and navigation principles remain consistent across all platforms:

Overview of Navigation 3 with advice on managing state, modularizing navigation code, and animation.

Migration from Navigation 2 to Navigation 3. It's easier to see Navigation 3 as a new library than a new version of the existing library, so it's less of a migration and more of a rewrite. But the guide points out the general steps to take.