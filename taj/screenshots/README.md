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
