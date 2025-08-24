# KRAIL <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Kotlin_Icon.png/1200px-Kotlin_Icon.png" height="30">  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Android_robot_head.svg/1100px-Android_robot_head.svg.png" height="30">  <img src="https://upload.wikimedia.org/wikipedia/commons/c/ca/IOS_logo.svg" height="30">

KRAIL is a modern, Compose Multiplatform app providing a seamless trip-planning experience across
Android and iOS. Built using Kotlin and Swift (for minor iOS-specific parts), the app offers
real-time public transport information, personalised features like trip-saving and themes, and
modular architecture for maintainability.

[![Krail App CI](https://github.com/ksharma-xyz/Krail/actions/workflows/build.yml/badge.svg)](https://github.com/ksharma-xyz/Krail/actions/workflows/build.yml)

## Table of Contents

- [Architecture](#architecture)
- [Core Technologies](#core-technologies)
- [App Features](#app-features)
- [Getting Started](#getting-started)
- [Building & Running](#building--running)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## Architecture

KRAIL follows a modular, cleanly separated architecture to ensure clarity, scalability, and
maintainability across platforms. Key modules include:

- `composeApp`: Main entry point and UI code for Android and shared Compose UI components.
- `iosApp`: iOS-specific entry point and platform adaptations.
- `core`: Shared business logic (domain layer).
- `feature/...`: Feature-specific modules (e.g. trip planner, discover).
- `gtfs-static`: Handling GTFS (General Transit Feed Specification) assets or prebuilt data.
- `taj`: Design system (Compose-based theming—not Material Design, but inspired by it).
- `sandook`: Local Database for offline storage.
- `core`: Core utilities and extensions e.g. appInfo, coroutines-ext, date-time, log, network.

---

## Core Technologies

| Area                     | Technology                                                                     |
|--------------------------|--------------------------------------------------------------------------------|
| **View Layer**           | Jetpack Compose + Compose Multiplatform                                        |
| **Dependency Injection** | Koin                                                                           |
| **Networking**           | `:core:network` module using Ktor (async HTTP client)                          |
| **Local Storage**        | `sandook` module SQLDelight (type-safe SQL)                                    |
| **Design System**        | `taj` module built using Compose foundation APIs inspired from Material Design |
| **Platform Management**  | Gradle Kotlin DSL, multiplatform targets                                       |

---

## App Features

- **Cross‑Platform UI**: Shared Compose UI logic across Android and iOS.
- **Real-Time Trip Planning**: Fetch and plan transit routes in Sydney and NSW.
- **Save Frequent Trips**: Quickly access saved trips for one‑tap planning.
- **Park & Ride Integration**: Discover nearby parking availability and integrate with train/metro
  journeys. See [:feature:park-ride](/feature/park-ride).
- **Custom Themes**: Match app appearance to user preferences.
  See [ThemeSelectionScreen.kt](/feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/themeselection/ThemeSelectionScreen.kt).
- **"Discover Sydney"** (v1.7.5 update): Explore local attractions, food spots, and events.

---

## Getting Started

### Prerequisites

- JDK 17+
- Kotlin SDK 2.1+
- Android Studio (latest stable)
- Xcode (for iOS development)
- API Keys
    - `NSW_TRIP_PLANNER_API_KEY` — obtain
      via [NSW Open Data developer platform.](https://opendata.transport.nsw.gov.au/)
- Firebase project (for analytics, crash reporting, etc.)
    - Put google-services.json in `composeApp/src/debug/` and `composeApp/src/release/`
    - Put GoogleService-Info.plist in `iosApp/iosApp`

---

## Building & Running

- **Clone the repository**:
  ```sh
  git clone git@github.com:ksharma-xyz/Krail.git
  ```

- **Build**:
  ```bash
  ./gradlew build
  ```

- **Run Tests**:
  ```bash
  ./gradlew test
  ```

- **Run App**:  
  Use Android Studio or Xcode to deploy on simulators or real devices.

---

## Contributing

Welcoming contributions from the community. Please create a new issue or pick up an existing one.
Please follow module structure and existing code patterns for consistency.

---

## Download

[<img src="https://i.imgur.com/M1RNcYP.png" alt="Download Krail App on Google Play Store" height="64"/>](https://play.google.com/store/apps/details?id=xyz.ksharma.krail)  [<img src="https://i.imgur.com/w8Ec7J4.png" alt="iOS Krail App on Apple App Store" height="64"/>](https://apps.apple.com/us/app/krail-app/id6738934832)

---

## License

```
Copyright 2024-2025 Karan Sharma.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Contact

- 🌐 Website: [krail.app](https://krail.app)
- 📧 Email: [hey@krail.app](mailto:hey@krail.app)
- 📘 Facebook: [facebook.com/krailapp](https://facebook.com/krailapp)
- 📸 Instagram: [instagram.com/krailapp](https://instagram.com/krailapp)
- 💼 LinkedIn: [linkedin.com/company/krail](https://www.linkedin.com/company/krail/)
- 👾 Reddit: [r/krailapp](https://www.reddit.com/r/krailapp/)

---
