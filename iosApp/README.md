# iOS App

## Debug Symbols (dSYM) and Crashlytics

For comprehensive documentation on working with dSYM files and uploading them to Firebase Crashlytics, see:

📖 **[iOS dSYM Files and Firebase Crashlytics Guide](../docs/ios-dsym-crashlytics.md)**

### Quick Reference

Find all dSYM files on your system:
```bash
mdfind -name .dSYM | while read -r line; do dwarfdump -u "$line"; done
```

dSYM files are automatically uploaded to Firebase Crashlytics during the build process via the configured build script.


