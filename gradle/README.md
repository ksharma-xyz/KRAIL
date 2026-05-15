# Gradle daemon JVM toolchain

## What `gradle-daemon-jvm.properties` is

It pins the JVM that **runs Gradle itself** (the build-tool daemon) to a
specific JDK: Oracle JDK 21. This is *not* the JDK that compiles the app
(that is the Kotlin/Java toolchain, configured separately in the build
scripts) — it is only the JVM the Gradle process runs on.

## Why it exists

Without this, the Gradle daemon runs on whatever JDK each contributor
happens to have on `JAVA_HOME` / `PATH`. That causes:

- **Environment drift** — different JDKs across machines and CI lead to
  inconsistent build behaviour and deprecation noise.
- **Daemon thrashing** — Gradle spawns a separate daemon per distinct
  JVM it sees, so an inconsistent setup means cold daemons and slower
  builds.
- **Onboarding friction** — new contributors otherwise have to install
  and point at the right JDK manually.

With the daemon JVM toolchain declared in-repo, every machine and CI
runner uses the **same** JDK. If it is missing locally, Gradle
auto-provisions it via the `foojay-resolver-convention` plugin (added in
`settings.gradle.kts`), which downloads JDKs from the foojay Disco API.

## Maintenance

- Bumping the daemon JDK: run `./gradlew updateDaemonJvm
  --jvm-version=<N> --jvm-vendor=<vendor>`; it regenerates
  `gradle-daemon-jvm.properties`. Do not hand-edit the URLs.
- The pinned vendor is Oracle. If the team standardises on a different
  distribution (Temurin, Zulu, etc.), regenerate with that vendor.
- First build on a machine without the pinned JDK incurs a one-time
  download. CI caches it after the first run.

## Not to be confused with

| Concept | Configures | Where |
|---|---|---|
| Daemon JVM toolchain | The JVM Gradle runs on | `gradle/gradle-daemon-jvm.properties` (this) |
| Java/Kotlin toolchain | The JDK that compiles + tests the code | build scripts (`jvmToolchain(...)` etc.) |
