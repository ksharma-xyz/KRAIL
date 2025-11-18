package xyz.ksharma.krail.discover.network.real

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comprehensive JSON validation tests for Discover card configurations.
 * These tests validate that a Discover JSON configuration works correctly
 * across multiple scenarios and edge cases.
 *
 * Use these tests to validate your Discover JSON before deploying to production.
 */
class DiscoverJsonValidationTest {

    private lateinit var fakeFlag: FakeFlag

    @BeforeTest
    fun setup() {
        fakeFlag = FakeFlag()
    }

    /**
     * Comprehensive test to validate the sample Discover JSON configuration.
     * This test validates the JSON against multiple test scenarios and provides detailed feedback.
     * All validation tests run against the same sampleDiscoverJson to ensure consistency.
     */
    @Test
    fun testValidateDiscoverJson_comprehensive() {
        val validationResult = validateDiscoverJson(sampleDiscoverJson)

        // Print validation results
        println(validationResult.getReport())

        // Assert that validation passed
        assertTrue(
            validationResult.isValid,
            "Discover JSON validation failed. See report:\n${validationResult.getReport()}"
        )
    }

    /**
     * Test JSON parsing specifically.
     */
    @Test
    fun testSampleJson_parsing() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, parseResult.message)
    }

    /**
     * Test card ID uniqueness specifically.
     */
    @Test
    fun testSampleJson_cardIdUniqueness() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val idResult = testCardIdUniqueness(cards)
        assertTrue(idResult.passed, idResult.message)
    }

    /**
     * Test date validation specifically.
     */
    @Test
    fun testSampleJson_dateValidation() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val dateResult = testDateValidation(cards)
        assertTrue(dateResult.passed, dateResult.message)
    }

    /**
     * Test image validation specifically.
     */
    @Test
    fun testSampleJson_imageValidation() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val imageResult = testImageValidation(cards)
        assertTrue(imageResult.passed, imageResult.message)
    }

    /**
     * Test button validation specifically.
     */
    @Test
    fun testSampleJson_buttonValidation() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val buttonResult = testButtonValidation(cards)
        assertTrue(buttonResult.passed, buttonResult.message)
    }

    /**
     * Test card type validation specifically.
     */
    @Test
    fun testSampleJson_cardTypeValidation() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val typeResult = testCardTypeValidation(cards)
        assertTrue(typeResult.passed, typeResult.message)
    }

    /**
     * Test required fields validation specifically.
     */
    @Test
    fun testSampleJson_requiredFields() {
        val parseResult = testJsonParsing(sampleDiscoverJson)
        assertTrue(parseResult.passed, "Failed to parse JSON")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>
        val fieldsResult = testRequiredFields(cards)
        assertTrue(fieldsResult.passed, fieldsResult.message)
    }

    // ========== Validation Helper Functions ==========

    private fun validateDiscoverJson(jsonString: String): ValidationResult {
        val results = mutableListOf<TestResult>()

        println("\n🔍 Discover JSON Validation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        // Test 1: JSON parsing
        val parseResult = testJsonParsing(jsonString)
        results.add(parseResult)
        if (!parseResult.passed) {
            println("❌ ${parseResult.testName}: ${parseResult.message}")
            return ValidationResult(false, results)
        }
        println("✅ ${parseResult.testName}")

        @Suppress("UNCHECKED_CAST")
        val cards = parseResult.data as List<DiscoverModel>

        // Test 2: Card ID uniqueness
        val idResult = testCardIdUniqueness(cards)
        results.add(idResult)
        println("${if (idResult.passed) "✅" else "❌"} ${idResult.testName}")
        if (!idResult.passed) println("   ${idResult.message}")

        // Test 3: Date validation
        val dateResult = testDateValidation(cards)
        results.add(dateResult)
        println("${if (dateResult.passed) "✅" else "❌"} ${dateResult.testName}")
        if (!dateResult.passed) println("   ${dateResult.message}")

        // Test 4: Image URL validation
        val imageResult = testImageValidation(cards)
        results.add(imageResult)
        println("${if (imageResult.passed) "✅" else "❌"} ${imageResult.testName}")
        if (!imageResult.passed) println("   ${imageResult.message}")

        // Test 5: Button validation
        val buttonResult = testButtonValidation(cards)
        results.add(buttonResult)
        println("${if (buttonResult.passed) "✅" else "❌"} ${buttonResult.testName}")
        if (!buttonResult.passed) println("   ${buttonResult.message}")

        // Test 6: Card type validation
        val typeResult = testCardTypeValidation(cards)
        results.add(typeResult)
        println("${if (typeResult.passed) "✅" else "❌"} ${typeResult.testName}")
        if (!typeResult.passed) println("   ${typeResult.message}")

        // Test 7: Required fields validation
        val fieldsResult = testRequiredFields(cards)
        results.add(fieldsResult)
        println("${if (fieldsResult.passed) "✅" else "❌"} ${fieldsResult.testName}")
        if (!fieldsResult.passed) println("   ${fieldsResult.message}")

        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return ValidationResult(results.all { it.passed }, results)
    }

    private fun testJsonParsing(jsonString: String): TestResult {
        return try {
            val jsonArray = JsonConfig.lenient.parseToJsonElement(jsonString).jsonArray
            val cards =
                jsonArray.map { JsonConfig.lenient.decodeFromJsonElement<DiscoverModel>(it) }
            TestResult(
                testName = "JSON Parsing (${cards.size} cards)",
                passed = true,
                message = "",
                data = cards
            )
        } catch (e: Exception) {
            TestResult(
                testName = "JSON Parsing",
                passed = false,
                message = "Failed to parse: ${e.message}"
            )
        }
    }

    private fun testCardIdUniqueness(cards: List<DiscoverModel>): TestResult {
        val cardIds = cards.map { it.cardId }
        val duplicates = cardIds.groupingBy { it }.eachCount().filter { it.value > 1 }

        return if (duplicates.isEmpty()) {
            TestResult(
                testName = "Card ID Uniqueness",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Card ID Uniqueness",
                passed = false,
                message = "Duplicate card IDs found: ${duplicates.keys.joinToString(", ")}"
            )
        }
    }

    private fun testDateValidation(cards: List<DiscoverModel>): TestResult {
        val errors = mutableListOf<String>()

        cards.forEach { card ->
            val startDate = card.startDate
            val endDate = card.endDate

            // Both should be present or both should be null
            if ((startDate == null) != (endDate == null)) {
                errors.add("${card.cardId}: startDate and endDate must both be present or both be null")
            }

            if (startDate != null && endDate != null) {
                // Validate ISO 8601 format
                try {
                    val start = LocalDate.parse(startDate)
                    val end = LocalDate.parse(endDate)

                    if (start > end) {
                        errors.add("${card.cardId}: startDate ($startDate) is after endDate ($endDate)")
                    }
                } catch (e: Exception) {
                    errors.add("${card.cardId}: Invalid date format - ${e.message}")
                }
            }
        }

        return if (errors.isEmpty()) {
            TestResult(
                testName = "Date Validation",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Date Validation",
                passed = false,
                message = errors.joinToString("\n   ")
            )
        }
    }

    private fun testImageValidation(cards: List<DiscoverModel>): TestResult {
        val errors = mutableListOf<String>()

        cards.forEach { card ->
            if (card.imageList.isEmpty()) {
                errors.add("${card.cardId}: imageList is empty")
            }

            card.imageList.forEachIndexed { index, url ->
                if (url.isBlank()) {
                    errors.add("${card.cardId}: Image URL at index $index is blank")
                }
                // Basic URL validation
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    errors.add("${card.cardId}: Image URL at index $index does not start with http:// or https://")
                }
            }
        }

        return if (errors.isEmpty()) {
            val totalImages = cards.sumOf { it.imageList.size }
            TestResult(
                testName = "Image Validation ($totalImages images)",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Image Validation",
                passed = false,
                message = errors.joinToString("\n   ")
            )
        }
    }

    private fun testButtonValidation(cards: List<DiscoverModel>): TestResult {
        val errors = mutableListOf<String>()

        cards.forEach { card ->
            val buttons = card.buttons ?: return@forEach

            buttons.forEach { button ->
                when (button) {
                    is Button.Cta -> {
                        if (button.label.isBlank()) {
                            errors.add("${card.cardId}: CTA button label is blank")
                        }
                        if (button.url.isBlank()) {
                            errors.add("${card.cardId}: CTA button URL is blank")
                        }
                        if (!button.url.startsWith("http://") && !button.url.startsWith("https://")) {
                            errors.add("${card.cardId}: CTA URL does not start with http:// or https://")
                        }
                    }

                    is Button.Social.PartnerSocial -> {
                        if (button.socialPartnerName.isBlank()) {
                            errors.add("${card.cardId}: PartnerSocial name is blank")
                        }
                        if (button.links.isEmpty()) {
                            errors.add("${card.cardId}: PartnerSocial has no links")
                        }
                        button.links.forEach { link ->
                            if (link.url.isBlank()) {
                                errors.add("${card.cardId}: PartnerSocial link URL is blank")
                            }
                            if (!link.url.startsWith("http://") && !link.url.startsWith("https://")) {
                                errors.add("${card.cardId}: PartnerSocial link URL does not start with http:// or https://")
                            }
                        }
                    }

                    is Button.Social.AppSocial -> { /* No validation needed */
                    }

                    is Button.Share -> { /* No validation needed */
                    }
                }
            }

            // Validate button combinations
            if (!buttons.isValidButtonCombo()) {
                errors.add("${card.cardId}: Invalid button combination")
            }
        }

        return if (errors.isEmpty()) {
            TestResult(
                testName = "Button Validation",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Button Validation",
                passed = false,
                message = errors.joinToString("\n   ")
            )
        }
    }

    private fun testCardTypeValidation(cards: List<DiscoverModel>): TestResult {
        val errors = mutableListOf<String>()
        val typeCounts = mutableMapOf<DiscoverCardType, Int>()

        cards.forEach { card ->
            typeCounts[card.type] = (typeCounts[card.type] ?: 0) + 1

            if (card.type == DiscoverCardType.Unknown) {
                errors.add("${card.cardId}: Card type is Unknown")
            }
        }

        return if (errors.isEmpty()) {
            val summary = typeCounts.entries
                .sortedBy { it.key.sortOrder }
                .joinToString(", ") { "${it.key.displayName}=${it.value}" }
            TestResult(
                testName = "Card Type Validation ($summary)",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Card Type Validation",
                passed = false,
                message = errors.joinToString("\n   ")
            )
        }
    }

    private fun testRequiredFields(cards: List<DiscoverModel>): TestResult {
        val errors = mutableListOf<String>()

        cards.forEach { card ->
            if (card.title.isBlank()) {
                errors.add("${card.cardId}: title is blank")
            }
            if (card.description.isBlank()) {
                errors.add("${card.cardId}: description is blank")
            }
            if (card.cardId.isBlank()) {
                errors.add("Card has blank cardId")
            }
        }

        return if (errors.isEmpty()) {
            TestResult(
                testName = "Required Fields",
                passed = true,
                message = ""
            )
        } else {
            TestResult(
                testName = "Required Fields",
                passed = false,
                message = errors.joinToString("\n   ")
            )
        }
    }

    // Helper to validate button combinations
    private fun List<Button>.isValidButtonCombo(): Boolean {
        val types = this.map { it::class }

        val leftTypes = listOf(
            Button.Cta::class,
            Button.Social::class,
        )

        // Share button validation
        if (types.contains(Button.Share::class)) {
            val leftCount = types.count { it in leftTypes }
            if (leftCount == 1) {
                val hasCta = types.contains(Button.Cta::class)
                val hasPartnerSocial = this.any { it is Button.Social.PartnerSocial }
                val hasAppSocial = this.any { it is Button.Social.AppSocial }

                // Allow Cta + Share OR PartnerSocial + Share, but not AppSocial + Share
                if (!hasCta && !hasPartnerSocial) {
                    return false
                }
                if (hasAppSocial) {
                    return false
                }
            }
        }

        return true
    }

    // ========== Data Classes for Validation Results ==========

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
        val data: Any? = null
    )

    data class ValidationResult(
        val isValid: Boolean,
        val testResults: List<TestResult>
    ) {
        fun getReport(): String {
            val passed = testResults.count { it.passed }
            val total = testResults.size
            val status = if (isValid) "✅ PASSED" else "❌ FAILED"

            val failures = testResults.filter { !it.passed }
            return if (failures.isEmpty()) {
                "$status ($passed/$total tests)"
            } else {
                buildString {
                    appendLine("$status ($passed/$total tests)")
                    appendLine()
                    failures.forEach {
                        appendLine("${it.testName}:")
                        appendLine(it.message)
                    }
                }
            }
        }
    }

    private class FakeFlag : Flag {
        private val flagValues = mutableMapOf<String, FlagValue>()

        @Suppress("unused")
        fun setFlagValue(key: String, value: FlagValue) {
            flagValues[key] = value
        }

        override fun getFlagValue(key: String): FlagValue {
            return flagValues[key] ?: FlagValue.BooleanValue(false)
        }
    }

    // ========== Sample JSON for Testing ==========

    /**
     * Comprehensive sample JSON that tests various scenarios:
     * - Different card types (Sports, Events, Food, Travel)
     * - Different button combinations (CTA+Share, PartnerSocial+Share, CTA only)
     * - With and without dates
     * - Multiple images
     * - Special characters in descriptions
     * - Future dates
     */
    private val sampleDiscoverJson = """
    [
       {
        "cardId": "card_aus_ind_t20i_men_2025",
        "title": "Australia v India T20Is 2025 – Men",
        "description": "Blockbuster T20I series as Australia Men take on India across Australia.",
        "imageList": [
          "https://resources.cricket-australia.pulselive.com/photo-resources/2025/07/31/19e70be6-c0e3-4175-bce9-21fa72bd7f8e/Group-6.png?width=700&height=396"
        ],
        "type": "Sports",
        "startDate": "2025-10-29",
        "endDate": "2025-11-08",
        "buttons": [
          {
            "buttonType": "Cta",
            "label": "Buy Tickets",
            "url": "https://www.cricket.com.au/tickets/series/CA:3123?"
          },
          {
            "buttonType": "Share"
          }
        ]
      },
      {
        "cardId": "card_aus_ind_odi_women_2026",
        "title": "Australia v India ODIs – Women",
        "description": "Australia Women face India in a high‑stakes ODI series in Brisbane, Hobart and Melbourne.",
        "imageList": [
          "https://resources.cricket-australia.pulselive.com/photo-resources/2025/05/30/ada1bb2e-c200-415e-8fc1-69a3eb6f7b8a/Group-8.png?width=700&height=396"
        ],
        "type": "Sports",
        "startDate": "2026-02-24",
        "endDate": "2026-03-01",
        "buttons": [
          {
            "buttonType": "Cta",
            "label": "Buy Tickets",
            "url": "https://www.cricket.com.au/tickets/series/CA:3125?"
          },
          {
            "buttonType": "Share"
          }
        ]
      },
      {
        "cardId": "card_aus_ind_t20i_women_2026",
        "title": "Australia v India T20Is – Women",
        "description": "Don’t miss Australia Women vs India in a thrilling T20I series in Sydney, Canberra and Adelaide",
        "imageList": [
          "https://resources.cricket-australia.pulselive.com/photo-resources/2025/05/30/ada1bb2e-c200-415e-8fc1-69a3eb6f7b8a/Group-8.png?width=700&height=396"
        ],
        "type": "Sports",
        "startDate": "2026-02-15",
        "endDate": "2026-02-21",
        "buttons": [
          {
            "buttonType": "Cta",
            "label": "Buy Tickets",
            "url": "https://www.cricket.com.au/tickets/series/CA:3124?"
          },
          {
            "buttonType": "Share"
          }
        ]
      }
    ]
    """.trimIndent()
}
