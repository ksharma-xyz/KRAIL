# Firebase Crashlytics - Automatic dSYM Upload Setup

This guide explains how to automatically upload dSYM files to Firebase Crashlytics in Xcode.

## Overview

**dSYM files** are required for Firebase Crashlytics to symbolicate crash reports (convert memory
addresses to readable code). Without them, crash reports show only memory addresses instead of file
names and line numbers.

## Current Setup

- **Firebase Integration**: Swift Package Manager (SPM)
- **Upload Script**: `Scripts/upload-crashlytics-symbols.sh`
- **GoogleService-Info.plist**: `iosApp/GoogleService-Info.plist`

---

## Automatic Upload Setup (Recommended)

### Step 1: Add Run Script Phase in Xcode

1. **Open Xcode project**:
   ```bash
   open /Users/ksharma/code/apps/KRAIL/iosApp/iosApp.xcodeproj
   ```

2. **Select the target**:
    - Click on the project in the navigator (top-left)
    - Select the **"iosApp"** target
    - Go to **"Build Phases"** tab

3. **Add new Run Script Phase**:
    - Click the **"+"** button at the top-left
    - Select **"New Run Script Phase"**
    - Drag it **AFTER** "Compile Sources" but **BEFORE** "Copy Bundle Resources"

4. **Configure the script**:

   **Script name:** `Upload dSYM to Firebase Crashlytics`

   **Shell:** `/bin/bash`

   **Script content:**
   ```bash
   "${PROJECT_DIR}/Scripts/upload-crashlytics-symbols.sh"
   ```

5. **Configure Input Files** (Important!):

   Click on "Input Files" and add:
   ```
   ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${TARGET_NAME}
   ${PROJECT_DIR}/${INFOPLIST_FILE}
   ```

6. **Configure settings**:
    - ✅ Check **"Run script only when installing"** (This ensures it only runs for Archive builds,
      not every build)
    - ✅ Check **"For install builds only"**
    - ⚠️ **UNCHECK** "Based on dependency analysis" (to ensure it always runs)

### Step 2: Verify the Setup

Run a test archive:

```bash
# In Xcode: Product → Archive
# Watch the build log for:
# "🔥 Firebase Crashlytics - Uploading dSYM files..."
# "✅ Successfully uploaded dSYM files to Firebase Crashlytics!"
```

---

## Manual Upload (For Testing or One-Time Upload)

### Option 1: Using the Script

```bash
# Navigate to iOS app directory
cd /Users/ksharma/code/apps/KRAIL/iosApp

# Run the upload script manually
./Scripts/upload-crashlytics-symbols.sh
```

### Option 2: Direct Command

```bash
# Find the upload-symbols tool
UPLOAD_TOOL=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -type f | head -1)

# Upload dSYMs from latest archive
"$UPLOAD_TOOL" \
  -gsp /Users/ksharma/code/apps/KRAIL/iosApp/iosApp/GoogleService-Info.plist \
  -p ios \
  ~/Library/Developer/Xcode/Archives/2026-01-06/iosApp*.xcarchive/dSYMs/
```

### Option 3: Upload from Desktop (Current dSYMs)

```bash
# Upload the dSYMs you just copied to Desktop
UPLOAD_TOOL=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -type f | head -1)

"$UPLOAD_TOOL" \
  -gsp /Users/ksharma/code/apps/KRAIL/iosApp/iosApp/GoogleService-Info.plist \
  -p ios \
  ~/Desktop/Krail_dSYMs_2026-01-06/
```

---

## Verifying dSYM Upload

### 1. Check Build Logs

After archiving, check the build log:

- Xcode → View → Navigators → Show Report Navigator (⌘9)
- Select your latest Archive
- Look for the "Upload dSYM to Firebase Crashlytics" phase
- Should see: `✅ Successfully uploaded dSYM files to Firebase Crashlytics!`

### 2. Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Crashlytics** → **Settings** (gear icon)
4. Look for uploaded dSYMs (might take a few minutes to appear)

### 3. Test Crash

After uploading dSYMs, test with a crash:

```swift
// Add to your app temporarily
fatalError("Test crash for Crashlytics")
```

Run the app, trigger the crash, reopen the app. Check Firebase Console in 5-10 minutes for a
symbolicated crash report.

---



---

## Advanced: Uploading Missing dSYMs

If you have old builds without uploaded dSYMs:

### Find all archives:

```bash
ls -lt ~/Library/Developer/Xcode/Archives/*/iosApp*.xcarchive/
```

### Upload specific archive:

```bash
UPLOAD_TOOL=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -type f | head -1)

# Replace with actual archive path
ARCHIVE_PATH="~/Library/Developer/Xcode/Archives/2026-01-06/iosApp 6-1-2026, 6.10 pm.xcarchive"

"$UPLOAD_TOOL" \
  -gsp /Users/ksharma/code/apps/KRAIL/iosApp/iosApp/GoogleService-Info.plist \
  -p ios \
  "$ARCHIVE_PATH/dSYMs/"
```

---

## CI/CD Integration

For automated builds (GitHub Actions, etc.), add this to your workflow:

```yaml
- name: Upload dSYMs to Crashlytics
  run: |
    # Find upload-symbols
    UPLOAD_SYMBOLS=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -type f | head -1)

    # Upload from archive
    "$UPLOAD_SYMBOLS" \
      -gsp ${{ github.workspace }}/iosApp/iosApp/GoogleService-Info.plist \
      -p ios \
      ~/Library/Developer/Xcode/Archives/*/iosApp*.xcarchive/dSYMs/
```
