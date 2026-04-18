# core:share

A Kotlin Multiplatform module that captures a Compose composable as an image and shares it via the native OS share sheet on Android and iOS.

---

## Module Structure

```
core/share/
  src/
    commonMain/   ← ShareManager interface + Koin expect module
    androidMain/  ← AndroidShareManager (FileProvider + Intent)
    iosMain/      ← IosShareManager (Skia encode + UIActivityViewController)
```

---

## How to Use

Inject `ShareManager` via Koin, capture the composable using `rememberGraphicsLayer()`, then call `shareImage`.

```kotlin
val shareManager: ShareManager = koinInject()
val graphicsLayer = rememberGraphicsLayer()
val scope = rememberCoroutineScope()

Box(
    modifier = Modifier.drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
    }
) {
    YourComposableContent()
}

Button(onClick = {
    scope.launch {
        val bitmap = graphicsLayer.toImageBitmap()
        shareManager.shareImage(bitmap)
            .onFailure { error ->
                // TODO: surface to UI when a snackbar / error state is wired up.
                println("ShareManager error: $error")
            }
    }
}) { Text("Share") }
```

> The composable is captured **in its current visual state** — collapsed, expanded, whatever is on screen at the time the button is tapped.

---

## Error Handling

`shareImage` returns `Result<Unit>`:

- `Result.success(Unit)` — the OS share sheet was presented successfully.
- `Result.failure(exception)` — something went wrong (see platform sections below for what can fail).

The call site currently logs failures. To show UI, replace the `println` with a state update:

```kotlin
shareManager.shareImage(bitmap)
    .onFailure { error -> viewModel.onShareError(error) }
```

---

## Platform Details

### Android

**Implementation:** `AndroidShareManager`

**Threading:**

| Step | Dispatcher | Why |
|---|---|---|
| `asAndroidBitmap()` + `compress()` + file write | `Dispatchers.IO` | Blocking disk I/O — must not block Main |
| `context.startActivity()` | `Dispatchers.Main` | UI operation — must be on Main |

**Flow:**
1. `ImageBitmap` → Android `Bitmap` via `asAndroidBitmap()` — a zero-copy unwrap of the underlying bitmap
2. Compressed to PNG and written to `context.cacheDir/share/journey_<timestamp>.png` on `Dispatchers.IO`
3. File exposed as a `content://` URI via `FileProvider` (authority: `${applicationId}.share.fileprovider`)
4. `Intent.ACTION_SEND` chooser launched on `Dispatchers.Main` with `FLAG_GRANT_READ_URI_PERMISSION`

**What can fail (captured by `runCatching`):**
- `Bitmap.compress()` returns `false` if the bitmap is recycled or hardware-backed → converted to an exception via `check()`
- `FileProvider.getUriForFile()` throws `IllegalArgumentException` if the file path is not covered by `share_file_paths.xml`
- `context.startActivity()` throws `ActivityNotFoundException` if no app on the device handles `ACTION_SEND image/png`

**Why FileProvider and not a plain `file://` URI?**
Android 7+ (API 24) blocks `file://` URIs across app boundaries. `FileProvider` converts the path to a `content://` URI that the receiving app can read without needing any storage permissions.

**Why a timestamped filename?**
Prevents a stale cached file from being served if the user taps Share twice in quick succession.

**Required host-app setup:**

`AndroidManifest.xml` — add inside `<application>`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.share.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/share_file_paths" />
</provider>
```

`res/xml/share_file_paths.xml`:
```xml
<paths>
    <cache-path name="shared_images" path="share/" />
</paths>
```

> The authority **must** match `${applicationId}.share.fileprovider` exactly — this is what `AndroidShareManager` passes to `FileProvider.getUriForFile(...)`.

---

### iOS

**Implementation:** `IosShareManager`

**Threading:**

| Step | Dispatcher | Why |
|---|---|---|
| Skia `encodeToData()` | `Dispatchers.Default` | CPU-heavy PNG encoding — must not block Main |
| `usePinned` + `NSData.create` + `UIImage(data:)` | (resume thread — Main if called from UI) | Cheap memory ops, no thread requirement |
| `UIActivityViewController` + `presentViewController` | `Dispatchers.Main` | UIKit is not thread-safe — must be on Main |

**Flow:**
1. `ImageBitmap` → Skia `Bitmap` via `asSkiaBitmap()` on `Dispatchers.Default`
2. Skia `Image.makeFromBitmap()` → `encodeToData(PNG, quality=100)` → raw `ByteArray`
3. `ByteArray` pinned in memory via `usePinned` → wrapped in `NSData` via C-interop
4. `UIImage(data: nsData)` created
5. `UIActivityViewController` presented from the **topmost** view controller on `Dispatchers.Main`

**What can fail (captured by `runCatching`):**
- `encodeToData()` returns `null` if the bitmap is empty or Skia cannot encode it → converted to an exception via `checkNotNull()`
- `UIImage(data:)` returns `null` if `NSData` is malformed → converted to an exception via `checkNotNull()`
- `topmostViewController()` returns `null` if `keyWindow` is nil (app in background or during lifecycle transitions) → converted to an exception via `checkNotNull()`

**Why Skia and not `toPixelMap()` + `CGBitmapContext`?**
Compose Multiplatform on iOS renders through Skia. `asSkiaBitmap()` directly accesses the underlying Skia buffer — zero colour conversion. A manual `CGBitmapContext` approach requires knowing the exact internal byte order (BGRA vs RGBA) and premultiplied-alpha convention that Skia uses. Getting it wrong produces shifted colours on simulator and crashes or corrupt images on real devices.

**Why walk up the VC stack instead of using `rootViewController` directly?**
On a real device, by the time the user taps Share the root view controller may already have something presented on top of it (a navigation push, a system permission dialog, etc.). iOS silently refuses to present a new view controller from one that is already presenting something else. Walking `presentedViewController` until `nil` guarantees we always find the topmost controller.

**Why `usePinned { ... NSData.create(bytes: pinned.addressOf(0), ...) }`?**
Kotlin/Native's garbage collector can move objects in memory. `usePinned` temporarily pins the `ByteArray` at a fixed address so the raw C pointer passed to `NSData.create(bytes:length:)` stays valid for the duration of the call.

---

## DI

`shareModule` is an `expect val` (commonMain) with `actual val` per platform:

- **Android** — `single<ShareManager> { AndroidShareManager(androidContext()) }`
- **iOS** — `single<ShareManager> { IosShareManager() }`

Register in your Koin setup: `modules(..., shareModule)`.

A Kotlin Multiplatform module that captures a Compose composable as an image and shares it via the native OS share sheet on Android and iOS.

---

## Module Structure

```
core/share/
  src/
    commonMain/   ← ShareManager interface + Koin expect module
    androidMain/  ← AndroidShareManager (FileProvider + Intent)
    iosMain/      ← IosShareManager (Skia encode + UIActivityViewController)
```

---

## How to Use

Inject `ShareManager` via Koin, capture the composable using `rememberGraphicsLayer()`, then call `shareImage`.

```kotlin
val shareManager: ShareManager = koinInject()
val graphicsLayer = rememberGraphicsLayer()
val scope = rememberCoroutineScope()

Box(
    modifier = Modifier.drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
    }
) {
    YourComposableContent()
}

Button(onClick = {
    scope.launch {
        val bitmap = graphicsLayer.toImageBitmap()
        shareManager.shareImage(bitmap)
    }
}) { Text("Share") }
```

> The composable is captured **in its current visual state** — collapsed, expanded, whatever is on screen at the time the button is tapped.

---

## Platform Details

### Android

**Implementation:** `AndroidShareManager`

**Flow:**
1. `ImageBitmap` → Android `Bitmap` via `asAndroidBitmap()` (Compose built-in, no pixel copying)
2. Compressed to PNG and saved to `context.cacheDir/share/journey_<timestamp>.png` on `Dispatchers.IO`
3. File exposed as a content URI via `FileProvider` (authority: `${applicationId}.share.fileprovider`)
4. `Intent.ACTION_SEND` chooser launched with `FLAG_GRANT_READ_URI_PERMISSION`

**Why FileProvider and not a plain `file://` URI?**  
Android 7+ (API 24) blocks `file://` URIs across app boundaries. `FileProvider` converts the path to a `content://` URI that the receiving app can read without needing any storage permissions.

**Why a timestamped filename?**  
Prevents a stale cached file from being served if the user shares twice in quick succession before the file is overwritten.

**Required host-app setup:**

`AndroidManifest.xml` — add inside `<application>`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.share.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/share_file_paths" />
</provider>
```

`res/xml/share_file_paths.xml`:
```xml
<paths>
    <cache-path name="shared_images" path="share/" />
</paths>
```

> The authority **must** match `${applicationId}.share.fileprovider` exactly — this is what `AndroidShareManager` passes to `FileProvider.getUriForFile(...)`.

---

### iOS

**Implementation:** `IosShareManager`

**Flow:**
1. `ImageBitmap` → Skia `Bitmap` via `asSkiaBitmap()` on `Dispatchers.Default`
2. Skia `Image.makeFromBitmap()` → `encodeToData(PNG)` → raw `ByteArray`
3. `ByteArray` pinned in memory via `usePinned` C-interop → wrapped in `NSData`
4. `UIImage(data: nsData)` created
5. `UIActivityViewController` presented from the **topmost** view controller on `Dispatchers.Main`

**Why Skia and not `toPixelMap()` + `CGBitmapContext`?**  
Compose Multiplatform on iOS renders through Skia. `asSkiaBitmap()` directly accesses the underlying Skia buffer — zero colour conversion. A manual `CGBitmapContext` approach requires knowing the exact internal byte order (BGRA vs RGBA) and premultiplied-alpha convention that Skia uses, and getting it wrong produces shifted colours on simulator and crashes or corrupt images on real devices.

**Why walk up the VC stack instead of using `rootViewController` directly?**  
On a real device, by the time the user taps Share the root view controller may already have something presented on top of it (a navigation push, a system permission dialog, the keyboard, etc.). iOS silently refuses to present a new view controller from one that is already presenting something else. Walking `presentedViewController` until `nil` guarantees we always find the topmost controller.

**Why `Dispatchers.Default` for encoding and `Dispatchers.Main` for presentation?**  
Skia PNG encoding is CPU-heavy and must not block the main thread. All UIKit calls (`UIActivityViewController`, `presentViewController`) must happen on the main thread — calling them from a background thread causes a crash or undefined behaviour on iOS.

**Why `usePinned { pinned -> NSData.create(bytes: pinned.addressOf(0), ...) }`?**  
Kotlin/Native's garbage collector can move objects in memory. `usePinned` temporarily pins the `ByteArray` at a fixed address so the raw C pointer passed to `NSData.create(bytes:length:)` stays valid for the duration of the call.

---

## Threading Summary

| Step | Thread | Reason |
|---|---|---|
| `graphicsLayer.toImageBitmap()` | Main (Compose draw) | Must be called from the composition thread |
| PNG encoding (iOS Skia) | `Dispatchers.Default` | CPU-heavy; must not block UI |
| File write (Android) | `Dispatchers.IO` | Blocking I/O; must not block UI |
| `Intent` / `UIActivityViewController` | Main | OS UI APIs require main thread |

---

## DI

`shareModule` is an `expect val` (commonMain) with `actual val` per platform:

- **Android** — `single<ShareManager> { AndroidShareManager(androidContext()) }`
- **iOS** — `single<ShareManager> { IosShareManager() }`

Register in your Koin setup: `modules(..., shareModule)`.

