### Wire - Protobuf

Run `./gradlew generateCommonMainProtos`, to generate Kotlin files form .proto files.
The files will be generated in the following dir: `build/generated/source/wire/`

## How to update NSW_STOPS.pb

1. Download the latest NSW_STOPS.pb file from [KRAIL-GTFS](https://github.com/ksharma-xyz/KRAIL-GTFS/blob/main/nswstops/NSW_STOPS.pb) repo
2. Replace the file in the `io/gtfs/src/commonMain/composeResources/files/NSW_STOPS.pb` directory
3. Increase version `NSW_STOPS_VERSION` value in `SandookPreferences` file.
That's it.
