# KRAIL Distribution Overview

This document provides a comparison of Android and iOS distribution workflows.

## Quick Reference

### Android → Google Play

**Manual Workflow**: `distribute-google-play-manual.yml`
```yaml
# Trigger: workflow_dispatch
# Input: track (internal, closed)
# Output: AAB uploaded to Google Play
```

**How to trigger**:
1. Go to Actions → Manual Build & Distribute Google Play
2. Select track (internal/closed)
3. Click Run workflow

### iOS → TestFlight

**Manual Workflow**: `distribute-testflight-manual.yml`
```yaml
# Trigger: workflow_dispatch
# Input: notify-testers (true/false)
# Output: IPA uploaded to TestFlight
```

**How to trigger**:
1. Go to Actions → Manual Build & Distribute TestFlight
2. Choose whether to notify testers
3. Click Run workflow

## Workflow Architecture

Both platforms follow the same pattern:

```
Manual Trigger Workflow
       ↓
Reusable Distribution Workflow
       ↓
Platform Store (Google Play / TestFlight)
```

### Android Workflows

1. **distribute-google-play-manual.yml** (Manual trigger)
   - Calls `build-android.yml` to build release AAB
   - Calls `distribute-google-play.yml` to upload

2. **distribute-google-play.yml** (Reusable)
   - Downloads AAB artifact
   - Uploads to Google Play using `r0adkll/upload-google-play` action

### iOS Workflows

1. **distribute-testflight-manual.yml** (Manual trigger)
   - Calls `distribute-testflight.yml` to build and upload

2. **distribute-testflight.yml** (Reusable)
   - Sets up code signing
   - Builds IPA using Fastlane
   - Uploads to TestFlight using Fastlane

## Distribution Tracks/Channels

### Android (Google Play)

- **internal**: Internal testing track (fastest)
- **closed**: Closed testing track
- **open**: Open testing track
- **production**: Production release

### iOS (TestFlight)

- **Internal Testing**: Automatic (up to 100 testers)
- **External Testing**: Requires App Review (up to 10,000 testers)
- **Production**: Separate App Store submission process

## Required Secrets

### Android

| Secret | Purpose |
|--------|---------|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Google Play API authentication |
| `ANDROID_NSW_TRANSPORT_API_KEY` | App API key |

### iOS

| Secret | Purpose |
|--------|---------|
| `APPSTORE_ISSUER_ID` | App Store Connect API authentication |
| `APPSTORE_KEY_ID` | App Store Connect API authentication |
| `APPSTORE_PRIVATE_KEY` | App Store Connect API authentication |
| `IOS_DIST_SIGNING_KEY_BASE64` | Code signing certificate (p12, base64) |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | Certificate password |
| `IOS_PROVISIONING_PROFILE_NAME` | Provisioning profile name |
| `IOS_NSW_TRANSPORT_API_KEY` | App API key |

## Build Artifacts

### Android
- **Format**: AAB (Android App Bundle)
- **Location**: `aab/*.aab`
- **Retention**: As configured in workflow

### iOS
- **Format**: IPA (iOS App Archive)
- **Location**: `iosApp/build/*.ipa`
- **Retention**: 30 days

## Common Use Cases

### Release to internal testers

**Android**:
```bash
# Via GitHub Actions UI
Select track: internal
```

**iOS**:
```bash
# Via GitHub Actions UI
Notify testers: false (internal testers get automatic access)
```

### Release to external testers

**Android**:
```bash
# Via GitHub Actions UI
Select track: closed
```

**iOS**:
```bash
# Via GitHub Actions UI
Notify testers: true
# Then in App Store Connect: Add to external testing group
```

## Local Testing

### Android
```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

### iOS
```bash
cd iosApp
bundle exec fastlane build_release
bundle exec fastlane upload_testflight
```

## Next Steps

### For Android
- ✅ Already set up and working
- Consider adding automated releases on tags

### For iOS
- ⬜ Complete setup (see `docs/ios-distribution.md`)
- ⬜ Configure GitHub secrets
- ⬜ Test manual workflow
- ⬜ Consider adding automated releases on tags

## Resources

- [Android Distribution Docs](https://developer.android.com/studio/publish)
- [iOS Distribution Docs](https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases)
- [Fastlane Documentation](https://docs.fastlane.tools/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

