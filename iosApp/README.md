# iOS App

## Locating dsym files
https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?hl=en&authuser=0&platform=ios

```
mdfind -name .dSYM | while read -r line; do dwarfdump -u "$line"; done
```


