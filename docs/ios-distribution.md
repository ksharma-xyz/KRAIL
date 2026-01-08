# iOS Distribution Setup

This document explains how to set up iOS distribution to TestFlight via GitHub Actions, similar to the Android Google Play distribution.

## Overview

The iOS distribution workflow allows you to:
- Manually trigger builds and uploads to TestFlight
- Distribute to internal testers (similar to Google Play Internal track)
- Optionally notify external testers

## Workflows

### 1. Manual Distribution (distribute-testflight-manual.yml)
**Trigger**: Manual via GitHub Actions UI
**Purpose**: Build and distribute iOS app to TestFlight on-demand

This is the main workflow you'll use, similar to `distribute-google-play-manual.yml` for Android.

### 2. Reusable Distribution (distribute-testflight.yml)
**Trigger**: Called by other workflows
**Purpose**: Reusable workflow for building and uploading to TestFlight

## Required Setup

### 1. App Store Connect API Key

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Navigate to **Users and Access** → **Keys** → **App Store Connect API**
3. Click **Generate API Key** or use an existing one
4. Download the `.p8` key file
5. Note the **Issuer ID**, **Key ID**, and save the **Private Key** content

### 2. Code Signing Certificate

1. Open **Keychain Access** on your Mac
2. Find your **Apple Distribution** certificate
3. Right-click → **Export** → Save as `.p12`
4. Set a password for the `.p12` file

### 3. Provisioning Profile

You can either:
- Use the automatic profile download (recommended - handled by the workflow)
- Or manually download from Apple Developer Portal

### 4. GitHub Secrets

Add the following secrets to your GitHub repository:

#### Required Secrets:

| Secret Name | Description | How to Get |
|------------|-------------|------------|
| `APPSTORE_ISSUER_ID` | App Store Connect API Issuer ID | From App Store Connect → Users and Access → Keys |
| `APPSTORE_KEY_ID` | App Store Connect API Key ID | From App Store Connect → Users and Access → Keys |
| `APPSTORE_PRIVATE_KEY` | App Store Connect API Private Key | Content of the downloaded `.p8` file |
| `IOS_DIST_SIGNING_KEY_BASE64` | Distribution certificate (base64 encoded) | `base64 -i YourCertificate.p12 \| pbcopy` |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | Password for the `.p12` certificate | Password you set when exporting |
| `IOS_PROVISIONING_PROFILE_NAME` | Name of provisioning profile | From Apple Developer Portal (e.g., "KRAIL AppStore Profile") |

#### Existing Secrets (already set up):
- `IOS_NSW_TRANSPORT_API_KEY`
- `ANDROID_NSW_TRANSPORT_API_KEY`
- `FIREBASE_IOS_GOOGLE_INFO`

## How to Use

### Manual Distribution to TestFlight

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select **Manual Build & Distribute TestFlight** workflow
4. Click **Run workflow**
5. Choose options:
   - **notify-testers**: Check to notify external testers (default: false)
6. Click **Run workflow**

The workflow will:
1. Build the iOS app for release
2. Sign the app with your distribution certificate
3. Upload to TestFlight
4. Save the IPA as an artifact

### Monitor Progress

- Watch the workflow run in the Actions tab
- Once complete, the build will appear in App Store Connect → TestFlight
- Internal testers can download and test immediately
- You can then promote to external testers or production as needed

## Comparison with Android

| Android (Google Play) | iOS (TestFlight) |
|----------------------|------------------|
| Manual trigger workflow | ✅ Manual trigger workflow |
| `distribute-google-play-manual.yml` | `distribute-testflight-manual.yml` |
| Reusable workflow | ✅ Reusable workflow |
| `distribute-google-play.yml` | `distribute-testflight.yml` |
| Track selection (internal/closed) | Notify testers option |
| Uses `upload-google-play` action | Uses Fastlane |
| AAB artifact | IPA artifact |

## Fastlane Setup

The project uses Fastlane for iOS builds and distribution:

- **Gemfile**: Ruby dependencies (Fastlane, CocoaPods)
- **iosApp/fastlane/Fastfile**: Build and upload lanes
- **iosApp/fastlane/Appfile**: App and team configuration

### Fastlane Lanes

- `build_release`: Builds the iOS app for App Store distribution
- `upload_testflight`: Uploads the built IPA to TestFlight
- `release`: Combines build and upload

## Troubleshooting

### Code Signing Issues

If you encounter code signing errors:
1. Verify your distribution certificate is valid
2. Check that the provisioning profile matches your bundle ID
3. Ensure the certificate and profile haven't expired

### Upload Failures

If TestFlight upload fails:
1. Verify your App Store Connect API credentials
2. Check that the app exists in App Store Connect
3. Ensure your bundle ID matches the app in App Store Connect

### Build Failures

If the build fails:
1. Check that all environment variables are set correctly
2. Verify the Xcode project configuration
3. Test the build locally using Fastlane: `cd iosApp && bundle exec fastlane build_release`

## Local Testing

To test the build locally:

```bash
# Install dependencies
bundle install

# Build the app
cd iosApp
bundle exec fastlane build_release

# Upload to TestFlight (requires API keys)
bundle exec fastlane upload_testflight

# Or do both
bundle exec fastlane release
```

## Next Steps

1. ✅ Set up required secrets in GitHub
2. ✅ Update `iosApp/fastlane/Appfile` with your Apple ID and Team IDs
3. ✅ Test manual workflow trigger
4. Consider setting up automatic distribution on main branch merges (similar to Android)

