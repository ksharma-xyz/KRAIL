# Taj Design System - Snapshot Tests

This directory contains snapshot tests for the Taj design system components.

## Quick Start

### Record Screenshots
```bash
./gradlew :taj:recordRoborazziDebug
```

### Verify Screenshots
```bash
./gradlew :taj:verifyRoborazziDebug
```

### Compare & Generate Report
```bash
./gradlew :taj:compareRoborazziDebug
```

## Adding New Snapshot Tests

### 1. Annotate Your Preview

In your component file (in `taj/src/commonMain` or `taj/src/androidMain`):

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.snapshot.ScreenshotTest

@ScreenshotTest
@Preview
@Composable
fun MyComponentPreview() {
    // Your preview content
    MyComponent()
}
```

### 2. Run Record Command

```bash
./gradlew :taj:recordRoborazziDebug
```

### 3. Check Generated Screenshots

Screenshots will appear in `taj/screenshots/`:
- `MyComponentPreview_light_normal.png` (Light mode, 1.0x font scale)
- `MyComponentPreview_light_xlarge.png` (Light mode, 2.0x font scale)
- `MyComponentPreview_dark_normal.png` (Dark mode, 1.0x font scale)

### 4. Commit Screenshots

```bash
git add taj/screenshots/
git commit -m "test: add snapshots for MyComponent"
```

That's it! 🎉

## File Naming Convention

Format: `{PreviewName}_{Theme}_{FontScale}.png`

Examples:
- `TextFieldPreview_light_normal.png`
- `TextFieldPreview_light_xlarge.png`
- `TextFieldPreview_dark_normal.png`
- `ThickDividerPreview_light_normal.png`

## Configuration

### Default Behavior

By default, each `@ScreenshotTest` preview generates **3 screenshots**:
1. **Light mode, normal** - 1.0x font scale
2. **Light mode, xlarge** - 2.0x font scale (accessibility)
3. **Dark mode, normal** - 1.0x font scale

### Custom Threshold

For components with animations or slight variations:

```kotlin
@ScreenshotTest(threshold = 0.01)  // Allow 1% difference
@Preview
@Composable
fun AnimatedPreview() { 
    AnimatedComponent()
}
```

### Different Preview Variants

Create separate preview functions for different states:

```kotlin
@ScreenshotTest
@Preview(name = "Empty State")
@Composable
fun ComponentEmptyPreview() {
    MyComponent(state = ComponentState.Empty)
}

@ScreenshotTest
@Preview(name = "Loading State")
@Composable
fun ComponentLoadingPreview() {
    MyComponent(state = ComponentState.Loading)
}

@ScreenshotTest
@Preview(name = "Success State")
@Composable
fun ComponentSuccessPreview() {
    MyComponent(state = ComponentState.Success(data))
}
```

### Landscape Mode

```kotlin
@ScreenshotTest
@Preview(device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun LandscapePreview() {
    MyComponent()
}
```

## Testing Checklist

When adding new components to Taj, create snapshots for:

- ✅ Default state
- ✅ Different sizes (if applicable)
- ✅ Disabled state (if applicable)
- ✅ Error state (if applicable)
- ✅ Loading state (if applicable)
- ✅ With/without icon (if applicable)
- ✅ Long text/overflow scenarios

These will automatically test:
- ✅ Light mode (1.0x and 2.0x font scales)
- ✅ Dark mode (1.0x font scale)

## Git LFS

Screenshots are tracked with Git LFS to keep repository size manageable.

### First Time Setup

```bash
# Install Git LFS
brew install git-lfs  # macOS

# Initialize in repo
git lfs install

# Pull screenshot files
git lfs pull
```

### Verifying LFS

```bash
# Check if file is tracked by LFS
git lfs ls-files | grep screenshots

# Check LFS status
git lfs status
```

## Troubleshooting

### "Screenshots not found" error
Run the record command first:
```bash
./gradlew :taj:recordRoborazziDebug
```

### Tests fail after UI changes
This is expected! Review the diff images in the build output, then update:
```bash
./gradlew :taj:recordRoborazziDebug
```

### No screenshots generated
- Verify `@ScreenshotTest` annotation is present
- Check the test is in `androidUnitTest` source set
- Run with `--rerun-tasks`: `./gradlew :taj:recordRoborazziDebug --rerun-tasks`
- Check console output for errors

### Wrong directory (taj/taj/screenshots)
The screenshots should be in `taj/screenshots/`, not `taj/taj/screenshots/`.
If you see nested directories, the `screenshotsDir` path is incorrect.

### Large file sizes
- Keep preview sizes reasonable
- Avoid full-screen previews unless necessary
- Use `threshold > 0` for components with dynamic content

## CI/CD

Screenshots are automatically verified in CI. Failed tests will:
1. Fail the build
2. Upload diff images as artifacts
3. Comment on PR with visual changes
4. Require manual review and update

## Related Files

- **Test file:** `src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TajSnapshotTest.kt`
- **Test previews:** `src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TestPreviews.kt`
- **Screenshots:** `screenshots/*.png`
- **Documentation:** `../../core/snapshot-testing/README.md`

## Examples

See `TestPreviews.kt` for example preview configurations:

```kotlin
@ScreenshotTest
@Preview
@Composable
fun TestPreview() {
    Box(modifier = Modifier.size(100.dp)) {
        Text("Test")
    }
}
```

## Support

For questions or issues:
1. Check [core/snapshot-testing/README.md](../../core/snapshot-testing/README.md)
2. Review existing test files in `src/androidUnitTest`
3. Ask in the team chat

