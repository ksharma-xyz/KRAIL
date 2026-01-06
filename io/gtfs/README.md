# GTFS Module

This module handles GTFS (General Transit Feed Specification) data for NSW transport.

## Documentation

- **[Bus Routes Architecture](BUS_ROUTES_ARCHITECTURE.md)** - Comprehensive guide to bus routes data flow, search implementation, and performance optimizations

## Wire - Protobuf

Run `./gradlew generateCommonMainProtos`, to generate Kotlin files form .proto files.
The files will be generated in the following dir: `build/generated/source/wire/`

## How to update NSW_STOPS.pb

1. Download the latest NSW_STOPS.pb file from [KRAIL-GTFS](https://github.com/ksharma-xyz/KRAIL-GTFS/blob/main/nswstops/NSW_STOPS.pb) repo
2. Replace the file in the `io/gtfs/src/commonMain/composeResources/files/NSW_STOPS.pb` directory
3. Increase version `NSW_STOPS_VERSION` value in `SandookPreferences` file.
That's it.

## How to update NSW_BUSES_ROUTES.pb

1. Download the latest NSW_BUSES_ROUTES.pb file from [KRAIL-GTFS](https://github.com/ksharma-xyz/KRAIL-GTFS/blob/main/nswbusroutes/NSW_BUSES_ROUTES.pb) repo
2. Replace the file in the `io/gtfs/src/commonMain/composeResources/files/NSW_BUSES_ROUTES.pb` directory
3. Increase version `NSW_BUS_ROUTES_VERSION` value in `SandookPreferences` file.
That's it.

See [Bus Routes Architecture](BUS_ROUTES_ARCHITECTURE.md) for detailed documentation on how bus routes data flows through the app.


