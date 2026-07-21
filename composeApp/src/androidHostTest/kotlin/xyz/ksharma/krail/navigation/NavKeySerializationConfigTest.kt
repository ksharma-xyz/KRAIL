package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModuleCollector
import xyz.ksharma.krail.feature.debug.settings.ui.navigation.DebugConfigRoute
import xyz.ksharma.krail.feature.track.ui.navigation.TrackRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerRoute
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.fail

/**
 * Guards the hand-maintained route registry in [krailNavSerializationConfig].
 *
 * Navigation 3 serializes the whole back stack in `onSaveInstanceState`, so a route missing
 * from the polymorphic module fails neither at build time nor on navigation — it throws the
 * moment the activity is recreated. In practice the screen works perfectly until the user
 * rotates the device, then the app dies.
 *
 * Adding a route and forgetting to register it is a one-line omission with no compiler help,
 * so this walks every sealed route hierarchy by reflection and fails with the exact missing
 * names. New routes are covered automatically; nobody has to remember to extend this test.
 */
class NavKeySerializationConfigTest {

    @Test
    fun `every route is registered for polymorphic serialization`() {
        val registered = registeredNavKeys()
        val unregistered = allRoutes().filterNot { it in registered }

        if (unregistered.isNotEmpty()) {
            fail(
                "These routes are not registered in krailNavSerializationConfig and will crash " +
                    "the app on configuration change (rotation, theme switch, font-size change):\n" +
                    unregistered.joinToString("\n") { "  - ${it.simpleName}" } +
                    "\n\nAdd a `subclass(X::class, X.serializer())` line for each in " +
                    "composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/" +
                    "SerializationConfig.kt",
            )
        }
    }

    /** Everything actually registered under `polymorphic(NavKey::class)`. */
    @OptIn(ExperimentalSerializationApi::class)
    private fun registeredNavKeys(): Set<KClass<*>> {
        val found = mutableSetOf<KClass<*>>()

        krailNavSerializationConfig.serializersModule.dumpTo(
            object : SerializersModuleCollector {
                override fun <T : Any> contextual(
                    kClass: KClass<T>,
                    provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
                ) = Unit

                override fun <Base : Any, Sub : Base> polymorphic(
                    baseClass: KClass<Base>,
                    actualClass: KClass<Sub>,
                    actualSerializer: KSerializer<Sub>,
                ) {
                    if (baseClass == NavKey::class) found += actualClass
                }

                override fun <Base : Any> polymorphicDefaultSerializer(
                    baseClass: KClass<Base>,
                    defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
                ) = Unit

                override fun <Base : Any> polymorphicDefaultDeserializer(
                    baseClass: KClass<Base>,
                    defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
                ) = Unit
            },
        )

        return found
    }

    /**
     * Every concrete route across the app's sealed route hierarchies. `sealedSubclasses` only
     * returns direct children, so nested hierarchies are walked recursively.
     */
    private fun allRoutes(): List<KClass<*>> =
        listOf(TripPlannerRoute::class, TrackRoute::class, DebugConfigRoute::class)
            .flatMap { it.concreteSubclasses() }

    private fun KClass<*>.concreteSubclasses(): List<KClass<*>> =
        sealedSubclasses.flatMap { subclass ->
            if (subclass.isSealed) subclass.concreteSubclasses() else listOf(subclass)
        }
}
