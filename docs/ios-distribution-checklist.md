# iOS Distribution Setup Checklist

Use this checklist to set up iOS distribution to TestFlight.

## 📋 Prerequisites

- [ ] macOS machine with Xcode installed
- [ ] Apple Developer Account (paid membership)
- [ ] App created in App Store Connect
- [ ] Access to GitHub repository settings

## 🔑 Step 1: Create App Store Connect API Key

1. - [ ] Go to [App Store Connect](https://appstoreconnect.apple.com)
2. - [ ] Navigate to **Users and Access** → **Keys** → **App Store Connect API**
3. - [ ] Click **Generate API Key** (or use existing)
   - Name: "KRAIL GitHub Actions"
   - Access: **Admin** or **App Manager**
4. - [ ] Download the `.p8` key file
5. - [ ] Save these values:
   - Issuer ID: `_________________`
   - Key ID: `_________________`
   - Private Key (content of .p8 file)

## 🔐 Step 2: Export Code Signing Certificate

### Option A: Using Xcode
1. - [ ] Open Xcode
2. - [ ] Go to **Settings/Preferences** → **Accounts**
3. - [ ] Select your Apple ID → **Manage Certificates**
4. - [ ] Create/verify you have an **Apple Distribution** certificate
5. - [ ] Open **Keychain Access**
6. - [ ] Find **Apple Distribution: Your Name (Team ID)**
7. - [ ] Right-click → **Export "Apple Distribution..."**
8. - [ ] Save as `krail-dist.p12`
9. - [ ] Set a password (save this for later)

### Option B: Using existing certificate
1. - [ ] Locate your existing `.p12` distribution certificate
2. - [ ] Note the password

### Convert to base64:
```bash
base64 -i krail-dist.p12 | pbcopy
```
This copies the base64 string to clipboard - save it for GitHub secrets.

## 🎫 Step 3: Note Provisioning Profile Name

1. - [ ] Go to [Apple Developer Portal](https://developer.apple.com/account)
2. - [ ] Navigate to **Certificates, IDs & Profiles** → **Profiles**
3. - [ ] Find or create an **App Store** provisioning profile for `xyz.ksharma.krail`
4. - [ ] Note the exact profile name: `_________________`

> **Note**: The workflow can auto-download profiles using API, but you need the name.

## 🔒 Step 4: Add GitHub Secrets

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**

Add these **Repository secrets**:

- [ ] `APPSTORE_ISSUER_ID` = (from Step 1)
- [ ] `APPSTORE_KEY_ID` = (from Step 1)
- [ ] `APPSTORE_PRIVATE_KEY` = (content of .p8 file from Step 1)
- [ ] `IOS_DIST_SIGNING_KEY_BASE64` = (base64 from Step 2)
- [ ] `IOS_DIST_SIGNING_KEY_PASSWORD` = (password from Step 2)
- [ ] `IOS_PROVISIONING_PROFILE_NAME` = (from Step 3)

### Verify existing secrets:
- [ ] `IOS_NSW_TRANSPORT_API_KEY` (should already exist)
- [ ] `FIREBASE_IOS_GOOGLE_INFO` (should already exist)

## ⚙️ Step 5: Update Configuration Files

### Update Fastlane Appfile

Edit `iosApp/fastlane/Appfile`:

```ruby
app_identifier("xyz.ksharma.krail")
apple_id("your-apple-id@email.com") # Your Apple Developer email

itc_team_id("XXXXXXXXXX") # App Store Connect Team ID
team_id("XXXXXXXXXX") # Developer Portal Team ID
```

To find your Team IDs:
- App Store Connect Team ID: App Store Connect → Account → Membership
- Developer Portal Team ID: developer.apple.com/account → Membership

- [ ] Updated `apple_id`
- [ ] Updated `itc_team_id`
- [ ] Updated `team_id`

## 🧪 Step 6: Test Locally (Optional but Recommended)

```bash
# Install dependencies
bundle install

# Test build locally
cd iosApp
bundle exec fastlane build_release
```

- [ ] Local build successful

## 🚀 Step 7: Test GitHub Actions Workflow

1. - [ ] Commit and push your changes
2. - [ ] Go to GitHub → **Actions**
3. - [ ] Select **Manual Build & Distribute TestFlight**
4. - [ ] Click **Run workflow**
5. - [ ] Set notify-testers: `false`
6. - [ ] Click **Run workflow**
7. - [ ] Monitor the workflow execution
8. - [ ] Verify build appears in App Store Connect → TestFlight

## ✅ Step 8: Verify TestFlight

1. - [ ] Go to [App Store Connect](https://appstoreconnect.apple.com)
2. - [ ] Navigate to **My Apps** → **KRAIL** → **TestFlight**
3. - [ ] Verify the build appears
4. - [ ] Add internal testers if needed
5. - [ ] Test the app!

## 🎉 Success!

You can now distribute iOS builds to TestFlight with a single click, just like Android!

## 🆘 Troubleshooting

If something goes wrong, check:
- [ ] All secrets are correctly copied (no extra spaces)
- [ ] Certificate and provisioning profile are valid and not expired
- [ ] Bundle ID matches exactly: `xyz.ksharma.krail`
- [ ] Team IDs are correct
- [ ] API key has proper permissions

See `docs/ios-distribution.md` for detailed troubleshooting.

## 📚 Resources

- Full docs: `docs/ios-distribution.md`
- Overview: `docs/distribution-overview.md`
- Setup script: `scripts/setup-ios-distribution.sh`

