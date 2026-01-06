#!/bin/bash

#
# Firebase Crashlytics dSYM Upload Script
# Automatically uploads debug symbols after archiving
#

echo "🔥 Firebase Crashlytics - Uploading dSYM files..."

# Path to the upload-symbols script (SPM)
UPLOAD_SYMBOLS_PATH="${BUILD_DIR%Build/*}SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/upload-symbols"

# Path to GoogleService-Info.plist
GOOGLE_SERVICE_INFO_PLIST="${PROJECT_DIR}/iosApp/GoogleService-Info.plist"

# Check if upload-symbols exists
if [ ! -f "$UPLOAD_SYMBOLS_PATH" ]; then
    echo "⚠️  Warning: upload-symbols script not found at: $UPLOAD_SYMBOLS_PATH"
    echo "Looking for alternative locations..."

    # Try to find it in DerivedData
    DERIVED_DATA_PATH=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -type f 2>/dev/null | head -1)

    if [ -f "$DERIVED_DATA_PATH" ]; then
        echo "✅ Found upload-symbols at: $DERIVED_DATA_PATH"
        UPLOAD_SYMBOLS_PATH="$DERIVED_DATA_PATH"
    else
        echo "❌ Error: Could not find upload-symbols script"
        exit 1
    fi
fi

# Check if GoogleService-Info.plist exists
if [ ! -f "$GOOGLE_SERVICE_INFO_PLIST" ]; then
    echo "❌ Error: GoogleService-Info.plist not found at: $GOOGLE_SERVICE_INFO_PLIST"
    exit 1
fi

# Find and upload the specific dSYM file
DSYM_FILE="${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}"

if [ ! -d "$DSYM_FILE" ]; then
    echo "❌ Error: dSYM file not found at: $DSYM_FILE"
    exit 1
fi

echo "📤 Uploading: ${DWARF_DSYM_FILE_NAME}"

# Upload the specific dSYM file (not the folder!)
"$UPLOAD_SYMBOLS_PATH" \
    -gsp "$GOOGLE_SERVICE_INFO_PLIST" \
    -p ios \
    "$DSYM_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Successfully uploaded dSYM files to Firebase Crashlytics!"
else
    echo "❌ Failed to upload dSYM files"
    exit 1
fi

