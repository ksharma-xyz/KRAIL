# Info Tile JSON Validator

## 🎯 Purpose

This validator test gives you **confidence before pushing JSON to remote config**. It validates:
- ✅ JSON syntax is correct
- ✅ All required fields are present  
- ✅ Date formats are valid (ISO-8601)
- ✅ Dates are logically sound (startDate ≤ endDate)
- ✅ Tile types are recognized
- ✅ URLs are properly formatted
- ✅ **Shows a preview of what users will see today**

## 🚀 Quick Start

### Run the Validator

```bash
# Run all validation tests
./gradlew :info-tile:network:real:test --tests "*InfoTileJsonValidatorTest*"

# Or run specific test
./gradlew :info-tile:network:real:test --tests "*InfoTileJsonValidatorTest.validJsonFile_simulateDisplayForToday"
```

### Example Output

```
============================================================
📅 DISPLAY SIMULATION FOR TODAY: 2026-02-21
============================================================

✅ VISIBLE TILES (2):

1. [CRITICAL_ALERT] Metro closure 21-22 Feb
   Key: metro_closure_feb_21_2026
   Start: 2026-02-21
   End: 2026-02-23
   Why showing: Active from Feb 21 to Feb 23

2. [INFO] New Trip Planning Features
   Key: new_feature_announcement
   Start: 2026-02-15
   End: No restriction
   Why showing: Started Feb 15 (-6 days ago), no end date

------------------------------------------------------------

⏳ HIDDEN TILES (1):

1. [CRITICAL_ALERT] Metro closure 7-8 Mar
   Key: metro_closure_mar_7_2026
   Start: 2026-03-07
   End: 2026-03-09
   Why hidden: Starts in 14 days (Mar 7)

============================================================
📊 SUMMARY: 2 visible, 1 hidden
============================================================
```

## 📋 Workflow: Before Pushing to Remote Config

### Step 1: Update Test JSON
Edit the JSON in `InfoTileJsonValidatorTest.kt`:
```kotlin
private fun loadValidJsonFixture(): String {
    return """
    [
      {
        "key": "your_new_tile",
        "title": "Your Title",
        "description": "Your description",
        "type": "CRITICAL_ALERT",
        "startDate": "2026-02-21",
        "endDate": "2026-02-23",
        "dismissCtaText": "Dismiss"
      }
    ]
    """.trimIndent()
}
```

### Step 2: Run Validation
```bash
./gradlew :info-tile:network:real:test --tests "*InfoTileJsonValidatorTest*"
```

### Step 3: Check Results

#### ✅ All Tests Pass
```
✅ SUCCESS: Parsed 3 valid tiles
✅ Tile 'metro_closure_feb_21_2026': All required fields present
✅ Tile 'metro_closure_feb_21_2026': startDate '2026-02-21' is valid
✅ Tile 'metro_closure_feb_21_2026': endDate '2026-02-23' is valid
✅ Tile 'metro_closure_feb_21_2026': Date range is logically valid
```
**→ Safe to push to Remote Config!**

#### ❌ Tests Fail
```
❌ FAILED: Tile 'invalid_tile': Invalid startDate format: 21/02/2026
```
**→ Fix the issues before pushing!**

### Step 4: Copy to Remote Config
Once validated, copy your JSON to Firebase Remote Config.

## 🧪 What Each Test Does

### 1. `validJsonFile_shouldParseSuccessfully`
**Validates:** JSON can be parsed without errors
```kotlin
✅ SUCCESS: Parsed 3 valid tiles
```

### 2. `validJsonFile_allTilesHaveRequiredFields`
**Validates:** Every tile has `key`, `title`, and `description`
```kotlin
✅ Tile 'metro_closure_feb_21_2026': All required fields present
```

### 3. `validJsonFile_allDatesAreValidFormat`
**Validates:** All dates are in ISO-8601 format (`YYYY-MM-DD`)
```kotlin
✅ Tile 'metro_closure_feb_21_2026': startDate '2026-02-21' is valid
✅ Tile 'metro_closure_feb_21_2026': endDate '2026-02-23' is valid
```

### 4. `validJsonFile_startDateIsBeforeOrEqualToEndDate`
**Validates:** startDate ≤ endDate (logical consistency)
```kotlin
✅ Tile 'metro_closure_feb_21_2026': Date range is logically valid (2026-02-21 to 2026-02-23)
```

### 5. `validJsonFile_urlsAreProperlyFormatted`
**Validates:** URLs start with `http://` or `https://`
```kotlin
✅ Tile 'metro_closure_feb_21_2026': URL is valid: https://transportnsw.info/...
```

### 6. `validJsonFile_simulateDisplayForToday` ⭐
**Validates:** Shows exactly what users will see today
- Lists visible tiles with reasons
- Lists hidden tiles with reasons
- Shows when future tiles will appear

### 7. `invalidJsonFile_shouldHandleGracefully`
**Validates:** Invalid JSON is caught and explained
```kotlin
🔍 INVALID JSON VALIDATION
============================================================

Checking tile 1: Invalid Date
  ❌ ERRORS found:
     - Invalid startDate format: 21/02/2026

Checking tile 2: Start After End
  ❌ ERRORS found:
     - startDate is after endDate
```

## 📝 Test Fixtures

### Valid Examples (`info_tiles_valid.json`)
Contains working examples you can use as templates:
- Metro closures with date ranges
- Feature announcements
- App updates
- One-day events
- Permanent tiles

### Invalid Examples (`info_tiles_invalid.json`)
Shows common mistakes to avoid:
- Wrong date formats
- Missing required fields
- Invalid tile types
- Logical errors (start after end)

## 🎨 Visual Preview

The validator shows tiles exactly as users will see them:

```
┌─────────────────────────────────────────────────┐
│ 🚨 Metro closure 21-22 Feb                     │
│                                                 │
│ No Metro Services will run between Tallawong   │
│ and Sydenham. Replacement buses will operate   │
│ between Tallawong and Chatswood.               │
│                                                 │
│ [Read more]                         [Dismiss]  │
└─────────────────────────────────────────────────┘
```

## ⚠️ Common Validation Errors

### Error: Invalid Date Format
```json
{
  "startDate": "21/02/2026"  // ❌ Wrong!
}
```
**Fix:** Use ISO-8601 format
```json
{
  "startDate": "2026-02-21"  // ✅ Correct
}
```

### Error: Start After End
```json
{
  "startDate": "2026-02-25",  // ❌ After end date
  "endDate": "2026-02-20"
}
```
**Fix:** Ensure startDate ≤ endDate
```json
{
  "startDate": "2026-02-20",  // ✅ Before end date
  "endDate": "2026-02-25"
}
```

### Error: Missing Required Field
```json
{
  "title": "My Tile"
  // ❌ Missing 'key' and 'description'
}
```
**Fix:** Include all required fields
```json
{
  "key": "my_tile",           // ✅ Required
  "title": "My Tile",          // ✅ Required
  "description": "Details..."  // ✅ Required
}
```

### Error: Invalid Tile Type
```json
{
  "type": "ALERT"  // ❌ Invalid type
}
```
**Fix:** Use valid tile types
```json
{
  "type": "CRITICAL_ALERT"  // ✅ Valid
  // or: "INFO", "APP_UPDATE", "INVITE_FRIENDS"
}
```

## 🔄 Continuous Integration

Add to your CI pipeline:
```yaml
# .github/workflows/ci.yml
- name: Validate Info Tiles
  run: ./gradlew :info-tile:network:real:test --tests "*InfoTileJsonValidatorTest*"
```

This ensures no invalid JSON gets merged!

## 💡 Tips

1. **Always run validator before pushing** to Remote Config
2. **Test with current date** to see what users see now
3. **Check "hidden tiles"** to verify future tiles will appear
4. **Use test fixtures** as templates for new tiles
5. **Add new scenarios** to test fixtures as you discover edge cases

## 📚 Related Files

- **Test:** `/info-tile/network/real/src/commonTest/.../InfoTileJsonValidatorTest.kt`
- **Fixtures:** `/info-tile/network/real/src/commonTest/resources/`
  - `info_tiles_valid.json` - Valid examples
  - `info_tiles_invalid.json` - Invalid examples
  - `README.md` - Detailed fixture documentation

## ✅ Pre-Push Checklist

Before pushing JSON to Remote Config:

- [ ] Run validator test
- [ ] All tests pass
- [ ] Checked display simulation (what users see today)
- [ ] Verified future tiles will appear at correct dates
- [ ] URLs are valid and accessible
- [ ] Dates are in ISO-8601 format
- [ ] startDate ≤ endDate (if both present)
- [ ] All required fields present
- [ ] Tile types are valid

## 🎯 Summary

This validator is your **safety net before pushing to production**. It:
- ✅ Catches errors before users see them
- ✅ Shows exactly what users will see
- ✅ Validates date logic and formatting
- ✅ Provides clear error messages
- ✅ Gives confidence in your remote config updates

**Run it every time before pushing to Remote Config!**

