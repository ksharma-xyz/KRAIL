package xyz.ksharma.krail.core.analytics.event

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `analytics-events.json` is the authoritative contract for KRAIL's analytics events.
 * KRAIL-Analytics generates its event registry, labels and metric groups from it, so if
 * this file and the contract disagree, that repo is silently wrong — which is exactly the
 * failure this contract exists to end.
 *
 * This test does not parse Kotlin. It instantiates every [AnalyticsEvent] subclass and
 * reads the real `properties` map — the same map the Firebase SDK sends. Parsing cannot
 * do that: `PROP_FROM_STOP_ID to tripFromStopId` emits the key `fromStopId` while the
 * constructor parameter is called `tripFromStopId`, and a parser records the wrong one.
 *
 * Optional params are found by instantiating twice: once with every nullable argument
 * populated, once with them null. Keys present only in the first pass are optional.
 */
class AnalyticsContractTest {

    private companion object {
        const val REGENERATE = "regenerateAnalyticsContract"
    }

    private val contract: JsonObject by lazy {
        val file = contractFile()
        assertTrue(file.exists(), "Contract not found at ${file.absolutePath}")
        Json.parseToJsonElement(file.readText()).jsonObject
    }

    @Test
    fun `every event in code is declared in the contract with the same params`() {
        val observed = AnalyticsEvent::class.sealedSubclasses.associate { subclass ->
            val required = instantiate(subclass, nullablesPopulated = false).properties.orEmpty().keys
            val full = instantiate(subclass, nullablesPopulated = true)
            full.name to ObservedEvent(
                className = subclass.simpleName ?: "?",
                params = full.properties.orEmpty().keys.associateWith { it !in required },
            )
        }

        if (System.getProperty(REGENERATE) != null) {
            regenerate(observed)
            return
        }

        val declared = contract["events"]!!.jsonArray.associate { element ->
            val obj = element.jsonObject
            obj["name"]!!.jsonPrimitive.content to obj["params"]!!.jsonArray.associate { p ->
                p.jsonObject["name"]!!.jsonPrimitive.content to
                    (p.jsonObject["optional"]!!.jsonPrimitive.content == "true")
            }
        }

        val problems = mutableListOf<String>()

        for ((name, event) in observed) {
            val expected = declared[name]
            if (expected == null) {
                problems += "$name (${event.className}) is in AnalyticsEvent.kt but not in analytics-events.json"
                continue
            }

            for (param in event.params.keys - expected.keys) {
                problems += "$name.$param is emitted but the contract does not declare it"
            }
            // A param the sample values never triggered is fine only if the contract admits
            // it is conditional — reflection uses fixed samples and cannot explore every
            // runtime branch, so `optional` is the honest way to record that.
            for (param in expected.keys - event.params.keys) {
                if (expected[param] != true) {
                    problems += "$name.$param is declared as always-present but the event never emits it"
                }
            }
            for ((param, isOptional) in event.params) {
                val declaredOptional = expected[param] ?: continue
                if (!declaredOptional && isOptional) {
                    problems += "$name.$param is only emitted for some inputs; mark it optional in the contract"
                }
                if (declaredOptional && !isOptional) {
                    problems += "$name.$param is always emitted; the contract marks it optional"
                }
            }
        }

        for (name in declared.keys - observed.keys) {
            problems += "$name is in analytics-events.json but no AnalyticsEvent subclass emits it"
        }

        if (problems.isNotEmpty()) {
            fail(
                "analytics-events.json is out of date with AnalyticsEvent.kt.\n" +
                    "Update the contract in this PR — KRAIL-Analytics generates its registry from it.\n" +
                    "To rewrite it from the code, run:\n" +
                    "  ./gradlew :core:analytics:testAndroidHostTest -D$REGENERATE=1\n" +
                    "then review the diff and fill in the label for any new event.\n\n" +
                    problems.sorted().joinToString("\n") { "  - $it" },
            )
        }
    }

    @Test
    fun `every event has a label for the dashboard`() {
        if (System.getProperty(REGENERATE) != null) return
        val unlabelled = contract["events"]!!.jsonArray
            .map { it.jsonObject }
            .filter { it["label"]?.jsonPrimitive?.content.isNullOrBlank() }
            .map { it["name"]!!.jsonPrimitive.content }
        assertEquals(
            emptyList(), unlabelled,
            "Every event needs a `label` in analytics-events.json — KRAIL-Analytics uses it " +
                "as the display name, and an unlabelled event shows up as a raw event name.",
        )
    }

    @Test
    fun `contract obeys Firebase naming and budget limits`() {
        val events = contract["events"]!!.jsonArray.map { it.jsonObject }
        val removed = contract["removed"]!!.jsonArray.map { it.jsonObject }
        val budget = contract["firebaseEventNameBudget"]!!.jsonPrimitive.content.toInt()

        val problems = mutableListOf<String>()
        val nameRule = Regex("^[a-z][a-z0-9_]*$")
        val reserved = listOf("firebase_", "google_", "ga_")

        for (event in events) {
            val name = event["name"]!!.jsonPrimitive.content
            if (!nameRule.matches(name)) problems += "event '$name' is not snake_case starting with a letter"
            if (name.length > 40) problems += "event '$name' exceeds Firebase's 40-character name limit"
            reserved.firstOrNull { name.startsWith(it) }
                ?.let { problems += "event '$name' uses the reserved prefix '$it'" }

            val params = event["params"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }
            if (params.size > 25) problems += "event '$name' declares ${params.size} params; Firebase allows 25"
            for (param in params) {
                if (!Regex("^[a-zA-Z][a-zA-Z0-9_]*$").matches(param)) problems += "$name.$param is not a legal parameter name"
                if (param.length > 40) problems += "$name.$param exceeds the 40-character parameter name limit"
                reserved.firstOrNull { param.startsWith(it) }
                    ?.let { problems += "$name.$param uses the reserved prefix '$it'" }
            }
        }

        // A name is a permanently spent Firebase slot, even after removal.
        val spent = events.size + removed.size
        if (spent > budget) problems += "$spent event names used against a budget of $budget"

        // Every replacement must point at something real, or the union that spans the
        // rename cannot be generated.
        val liveNames = events.map { it["name"]!!.jsonPrimitive.content }.toSet()
        for (entry in removed) {
            val replacedBy = entry["replacedBy"]?.jsonPrimitive?.contentOrNullSafe() ?: continue
            if (replacedBy !in liveNames) {
                problems += "removed event '${entry["name"]!!.jsonPrimitive.content}' claims to be replaced by " +
                    "'$replacedBy', which is not a live event"
            }
        }

        assertEquals(emptyList(), problems, "analytics-events.json violates Firebase constraints")
    }

    // ── regeneration ───────────────────────────────────────────────────────

    private data class ObservedEvent(val className: String, val params: Map<String, Boolean>)

    /**
     * Rewrite the contract from the code, preserving everything reflection cannot know:
     * labels, descriptions, metric membership, and the removed/removedParams history.
     * Params the sample values never triggered are kept and forced to optional, since
     * they are real but runtime-conditional.
     */
    private fun regenerate(observed: Map<String, ObservedEvent>) {
        val existing = contract["events"]!!.jsonArray.associate { it.jsonObject["name"]!!.jsonPrimitive.content to it.jsonObject }

        val events = observed.entries.sortedBy { it.key }.map { (name, event) ->
            val prev = existing[name]
            val prevParams = prev?.get("params")?.jsonArray
                ?.associate { it.jsonObject["name"]!!.jsonPrimitive.content to it.jsonObject }
                .orEmpty()

            val params = buildJsonArray {
                val names = (event.params.keys + prevParams.keys).sorted()
                for (param in names) {
                    val emittedOptional = event.params[param]
                    add(
                        buildJsonObject {
                            put("name", param)
                            put("optional", emittedOptional ?: true)
                        },
                    )
                }
            }

            buildJsonObject {
                put("name", name)
                put("class", event.className)
                put("label", prev?.get("label")?.jsonPrimitive?.content ?: "")
                prev?.get("description")?.let { put("description", it) }
                prev?.get("metric")?.let { put("metric", it) }
                put("params", params)
            }
        }

        val out = buildJsonObject {
            for ((key, value) in contract) {
                if (key == "events") put("events", JsonArray(events)) else put(key, value)
            }
        }

        val pretty = Json { prettyPrint = true; prettyPrintIndent = "  " }
        contractFile().writeText(pretty.encodeToString(JsonObject.serializer(), out) + "\n")
        println("Regenerated ${contractFile().absolutePath} with ${events.size} events.")
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this.toString() == "null") null else content

    /**
     * Test working directory is the module dir under Gradle, but fall back to walking up
     * so the test also runs from an IDE run configuration rooted at the repo.
     */
    private fun contractFile(): File {
        val direct = File("analytics-events.json")
        if (direct.exists()) return direct
        return File("core/analytics/analytics-events.json")
    }

    private fun instantiate(subclass: KClass<out AnalyticsEvent>, nullablesPopulated: Boolean): AnalyticsEvent {
        subclass.objectInstance?.let { return it }
        val ctor = subclass.primaryConstructor
            ?: fail("${subclass.simpleName} has no primary constructor and is not an object")

        val args = ctor.parameters.associateWith { param ->
            if (param.type.isMarkedNullable && !nullablesPopulated) null
            else sampleFor(param, subclass)
        }.filterNot { (param, value) ->
            // Let defaulted params keep their default when we have nothing better.
            value == null && param.isOptional && !param.type.isMarkedNullable
        }
        return ctor.callBy(args)
    }

    private fun sampleFor(param: KParameter, owner: KClass<*>, depth: Int = 0): Any? {
        val classifier = param.type.classifier as? KClass<*>
            ?: fail("${owner.simpleName}.${param.name}: unsupported type ${param.type}")

        return when {
            classifier == String::class -> "sample"
            classifier == Boolean::class -> true
            classifier == Int::class -> 1
            classifier == Long::class -> 1L
            classifier == Double::class -> 1.0
            classifier == Float::class -> 1.0f
            classifier.java.isEnum -> classifier.java.enumConstants.first()
            // Collections only affect the emitted VALUE, never the key set, so empty is safe.
            classifier == Set::class -> emptySet<Any>()
            classifier == List::class || classifier == Collection::class -> emptyList<Any>()
            classifier == Map::class -> emptyMap<Any, Any>()
            classifier.isSubclassOf(AnalyticsScreen::class) ->
                AnalyticsScreen::class.sealedSubclasses.first().objectInstance
            classifier.objectInstance != null -> classifier.objectInstance
            classifier.sealedSubclasses.isNotEmpty() ->
                classifier.sealedSubclasses.first().objectInstance
                    ?: buildNested(classifier.sealedSubclasses.first(), owner, param, depth)
            // A plain data class used as a parameter — build one recursively.
            classifier.primaryConstructor != null -> buildNested(classifier, owner, param, depth)
            else -> fail(
                "${owner.simpleName}.${param.name}: no sample value for ${classifier.simpleName}. " +
                    "Add a case to sampleFor() — skipping would let the contract drift unnoticed.",
            )
        }
    }

    private fun buildNested(target: KClass<*>, owner: KClass<*>, param: KParameter, depth: Int): Any {
        if (depth > 4) {
            fail("${owner.simpleName}.${param.name}: ${target.simpleName} nests more than four levels deep")
        }
        val ctor = target.primaryConstructor
            ?: fail("${owner.simpleName}.${param.name}: ${target.simpleName} has no primary constructor")
        val args = ctor.parameters
            .filterNot { it.isOptional }
            .associateWith { nested ->
                if (nested.type.isMarkedNullable) null else sampleFor(nested, target, depth + 1)
            }
        return ctor.callBy(args)
    }
}
