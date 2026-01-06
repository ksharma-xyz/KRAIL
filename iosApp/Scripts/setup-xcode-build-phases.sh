#!/bin/bash

#
# Xcode Build Phase Auto-Setup Script
# Automatically adds the Firebase Crashlytics dSYM upload script to Xcode project
# No manual clicking required!
#
# Usage: ./setup-xcode-build-phases.sh
#

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PBXPROJ_FILE="${PROJECT_DIR}/iosApp.xcodeproj/project.pbxproj"

echo "🔧 Setting up Xcode Build Phases..."
echo "📁 Project: ${PROJECT_DIR}"

# Check if project file exists
if [ ! -f "$PBXPROJ_FILE" ]; then
    echo "❌ Error: project.pbxproj not found at: $PBXPROJ_FILE"
    exit 1
fi

# Check if script already exists in project
if grep -q "Upload dSYM to Firebase Crashlytics" "$PBXPROJ_FILE"; then
    echo "✅ Build phase already exists - nothing to do!"
    exit 0
fi

echo "📝 Adding 'Upload dSYM to Firebase Crashlytics' build phase..."

# Create a backup
cp "$PBXPROJ_FILE" "${PBXPROJ_FILE}.backup"
echo "💾 Backup created: ${PBXPROJ_FILE}.backup"

# Generate a unique ID for the build phase (Xcode uses 24-character hex IDs)
BUILD_PHASE_ID="5DDDAA6B2F0CF3D700$(date +%s | tail -c 7)"

# Python script to add the build phase
python3 - <<EOF
import re
import sys

# Read the project file
with open('${PBXPROJ_FILE}', 'r') as f:
    content = f.read()

# Find the PBXNativeTarget section for iosApp
target_pattern = r'(/\* iosApp \*/ = \{[^}]+buildPhases = \(([^)]+)\);)'
match = re.search(target_pattern, content, re.DOTALL)

if not match:
    print("❌ Error: Could not find iosApp target")
    sys.exit(1)

# Get the buildPhases array
build_phases = match.group(2).strip().split('\n')
build_phases = [phase.strip() for phase in build_phases if phase.strip()]

# Find "Compile Sources" phase and insert our phase after it
compile_sources_found = False
new_build_phases = []
for phase in build_phases:
    new_build_phases.append(phase)
    if 'Sources */' in phase:
        compile_sources_found = True
        # Insert our new phase ID after Compile Sources
        new_build_phases.append(f"\t\t\t\t${BUILD_PHASE_ID} /* Upload dSYM to Firebase Crashlytics */,")

if not compile_sources_found:
    print("⚠️  Warning: Could not find 'Compile Sources' phase, appending to end")
    new_build_phases.append(f"\t\t\t\t${BUILD_PHASE_ID} /* Upload dSYM to Firebase Crashlytics */,")

# Replace the buildPhases array
new_phases_str = '\n'.join(new_build_phases)
content = content.replace(match.group(2).strip(), new_phases_str.strip())

# Add the PBXShellScriptBuildPhase section
script_section = f'''
/* Begin PBXShellScriptBuildPhase section */
\t\t${BUILD_PHASE_ID} /* Upload dSYM to Firebase Crashlytics */ = {{
\t\t\tisa = PBXShellScriptBuildPhase;
\t\t\talwaysOutOfDate = 1;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
\t\t\t);
\t\t\tinputFileListPaths = (
\t\t\t);
\t\t\tinputPaths = (
\t\t\t\t"${{DWARF_DSYM_FOLDER_PATH}}/${{DWARF_DSYM_FILE_NAME}}/Contents/Resources/DWARF/${{TARGET_NAME}}",
\t\t\t\t"${{PROJECT_DIR}}/${{INFOPLIST_FILE}}",
\t\t\t);
\t\t\tname = "Upload dSYM to Firebase Crashlytics";
\t\t\toutputFileListPaths = (
\t\t\t);
\t\t\toutputPaths = (
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 1;
\t\t\tshellPath = /bin/bash;
\t\t\tshellScript = "\\"\\${{PROJECT_DIR}}/Scripts/upload-crashlytics-symbols.sh\\"\\n";
\t\t\tshowEnvVarsInLog = 0;
\t\t}};
/* End PBXShellScriptBuildPhase section */
'''

# Insert the script section before "/* Begin PBXSourcesBuildPhase section */"
if '/* Begin PBXShellScriptBuildPhase section */' in content:
    # Already has shell script phases, add to existing section
    content = re.sub(
        r'(/\* Begin PBXShellScriptBuildPhase section \*/)',
        r'\1' + script_section.replace('/* Begin PBXShellScriptBuildPhase section */', '').replace('/* End PBXShellScriptBuildPhase section */', ''),
        content
    )
else:
    # No shell script phases yet, create new section
    content = re.sub(
        r'(/\* Begin PBXSourcesBuildPhase section \*/)',
        script_section + r'\n\1',
        content
    )

# Write back
with open('${PBXPROJ_FILE}', 'w') as f:
    f.write(content)

print("✅ Build phase added successfully!")
EOF

if [ $? -eq 0 ]; then
    echo "✅ Xcode project configured!"
    echo ""
    echo "📋 What was done:"
    echo "   ✓ Added 'Upload dSYM to Firebase Crashlytics' build phase"
    echo "   ✓ Positioned after 'Compile Sources'"
    echo "   ✓ Set to run only for Archive builds (deployment)"
    echo "   ✓ Backup saved at: ${PBXPROJ_FILE}.backup"
    echo ""
    echo "🎉 Setup complete! No manual Xcode configuration needed."
    echo "   Next archive will automatically upload dSYMs to Firebase."
else
    echo "❌ Failed to configure Xcode project"
    echo "🔄 Restoring from backup..."
    mv "${PBXPROJ_FILE}.backup" "$PBXPROJ_FILE"
    exit 1
fi
EOF

