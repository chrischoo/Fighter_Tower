# Dependency and SDK Update Plan

This plan updates various dependencies, plugins, and SDK versions as identified by the project's code analysis.

## User Review Required

> [!IMPORTANT]
> - **SDK Update**: Updating `compileSdk` and `targetSdk` to 37 (as suggested by analysis) may introduce behavior changes. Ensure you have the corresponding SDK platforms installed.
> - **Major Version Jumps**: Several libraries (Activity, Lifecycle, Compose BOM) are being updated across multiple major versions. This may require code changes if any APIs have been deprecated or removed.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///E:/StudioProjects/Fighter_Tower/app/build.gradle.kts)
- Update Android Gradle Plugin (AGP) from `8.13.2` to `9.3.2`.
- Update Kotlin plugin from `2.0.21` to `2.4.10`.
- Update `compileSdk` to `37` and `targetSdk` to `37`.
- Update `androidx.core:core-ktx` to `1.19.0`.
- Update `androidx.activity:activity-compose` to `1.13.0`.
- Update `androidx.lifecycle:lifecycle-runtime-ktx` and `lifecycle-viewmodel-compose` to `2.11.0`.
- Update `androidx.compose:compose-bom` to `2026.08.00`.

#### [MODIFY] [game-core/build.gradle.kts](file:///E:/StudioProjects/Fighter_Tower/game-core/build.gradle.kts)
- Update Kotlin JVM plugin from `2.0.21` to `2.4.10`.

## Verification Plan

### Automated Tests
- Run `gradle sync` to verify dependency resolution.
- Execute unit tests in `:game-core` using `./gradlew :game-core:test`.
- Build the app using `./gradlew :app:assembleDebug`.

### Manual Verification
- Verify the project builds successfully in Android Studio without sync errors.
