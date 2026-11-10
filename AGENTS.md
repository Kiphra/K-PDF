# Agent Guidelines (AI-ready) — K-PDF: PDF Editor & Tools

This project is a modern Android app dedicated to **Google Play Store**. Use this file as the **source of truth** for how to build, test, and release the application.

## Repo overview

- **Android app**: `app/`
- **Build Target**: Dedicated Google Play Store release (uses Google ML Kit, GMS Document Scanner, and Google Play In-App Review)
- **Version source of truth (for releases)**: `gradle.properties`
  - `APP_VERSION_CODE`
  - `APP_VERSION_NAME`
- **Fastlane metadata**: `fastlane/metadata/android/en-US/`

## Building locally (developer machine)

- **Requirements**: JDK 17+, Android SDK installed, Gradle wrapper available.
- **Common commands**:
  - `./gradlew :app:assembleDebug` — Build debug APK
  - `./gradlew :app:assembleRelease` — Build production APK
  - `./gradlew :app:bundleRelease` — Build production Android App Bundle (AAB)
  - `./gradlew :app:lintDebug --continue` — Run Android lint
  - `./gradlew :app:testDebugUnitTest` — Run unit tests

## Versioning rules

Static version values are maintained in `gradle.properties`:

- `APP_VERSION_CODE=<int>`
- `APP_VERSION_NAME=<string>`

`app/build.gradle.kts` reads these properties for `versionCode`/`versionName`.

## PDF viewer / annotation rules (don’t regress)

### UI/UX guidelines
- **Highlighter tool** must be transparent and blend with content (not opaque marker).
  - Use `BlendMode.MULTIPLY` or `PorterDuff.Mode.MULTIPLY`.
  - Use ~30% alpha.
  - Avoid stacking: render highlights in a single layer.
- **Zoom controls**: keep zoom button container hidden; preserve menu access.
- **Touch gestures**: don’t break pinch-to-zoom; allow multi-touch to reach the PDF view.

### Fragment lifecycle and overlays
- Set `documentUri` before attaching the viewer fragment.
- Use `view.post {}` for UI changes that require the PDF to be loaded.
- Overlay views should not intercept touch unless actively annotating.

## What an AI agent should do when editing

- Maintain Google Play compliance, GDPR/CCPA privacy standards, and on-device privacy guarantee.
- When updating versions:
  - Update `APP_VERSION_CODE` / `APP_VERSION_NAME` in `gradle.properties`.
  - Add changelog in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
  - Update `CHANGELOG.md`.

---

Last Updated: 2026-11-11
