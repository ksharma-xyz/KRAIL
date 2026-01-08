#!/bin/bash

# iOS Distribution Setup Script
# This script helps set up the iOS distribution workflow

echo "🍎 KRAIL iOS Distribution Setup"
echo "================================"
echo ""

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
  echo "⚠️  Warning: This script should be run on macOS for full functionality"
fi

# Install Ruby dependencies
echo "📦 Installing Ruby dependencies..."
if command -v bundle &> /dev/null; then
  bundle install
  echo "✅ Ruby dependencies installed"
else
  echo "❌ Bundler not found. Please install it first:"
  echo "   gem install bundler"
  exit 1
fi

echo ""
echo "✅ Setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Configure GitHub Secrets (see docs/ios-distribution.md)"
echo "2. Update iosApp/fastlane/Appfile with your Apple ID and Team IDs"
echo "3. Test the workflow: Actions → Manual Build & Distribute TestFlight"
echo ""
echo "📖 Full documentation: docs/ios-distribution.md"

