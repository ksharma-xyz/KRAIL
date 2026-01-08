#!/bin/bash

echo "🧪 Testing iOS Distribution Locally"
echo "===================================="
echo ""

# Check Ruby version
echo "1️⃣ Checking Ruby version..."
ruby --version
if [ $? -ne 0 ]; then
  echo "❌ Ruby not found. Please install Ruby 4.0.0"
  exit 1
fi
echo ""

# Check Bundler
echo "2️⃣ Checking Bundler..."
if ! command -v bundle &> /dev/null; then
  echo "📦 Installing Bundler..."
  gem install bundler
fi
bundle --version
echo ""

# Install dependencies
echo "3️⃣ Installing Fastlane dependencies..."
bundle install
if [ $? -ne 0 ]; then
  echo "❌ Bundle install failed"
  exit 1
fi
echo ""

# Test Fastlane
echo "4️⃣ Testing Fastlane..."
cd iosApp
bundle exec fastlane --version
if [ $? -ne 0 ]; then
  echo "❌ Fastlane not working"
  exit 1
fi
echo ""

# Show available lanes
echo "5️⃣ Available Fastlane lanes:"
bundle exec fastlane lanes
echo ""

echo "✅ Local setup is working!"
echo ""
echo "📝 To test build locally (requires certificates):"
echo "   cd iosApp"
echo "   bundle exec fastlane build_release"
echo ""
echo "⚠️  Note: Building requires:"
echo "   - Valid Apple Distribution certificate in Keychain"
echo "   - App Store provisioning profile"
echo "   - Environment variable: IOS_NSW_TRANSPORT_API_KEY"

