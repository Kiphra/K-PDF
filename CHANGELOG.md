# Changelog

All notable changes to the K-PDF project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] - 2026-11-11

### Added
- Complete brand evolution to **K-PDF: PDF Editor & Tools** published by **Kiphra**.
- Engineering-grade obsidian (`#0D0E12`) and crimson (`#E53935`) dark aesthetic across all screens and components.
- Advanced 4-point perspective warp and edge detection for Scan to PDF.
- Print Imposition tool with Cut & Stack, N-Up, 2-Up, 4-Up, and RTL/LTR layout modes.
- Universal mailto client handler pre-filling support diagnostics.
- Web Privacy Policy and Third-Party Software Notices hosted at `https://kiphra.github.io/K-PDF/`.

### Changed
- Standardized application package ID to `com.kiphra.kpdf`.
- Migrated default PDF output location to `Documents/K-PDF`.
- Updated CI deployment workflows and store release metadata.

---

## [1.3.210] - 2026-08-08

### Added
- Created `SafeLauncher.kt` extension abstraction (`safeLaunch`) to safely launch all `ActivityResultLauncher` intents.
- Centralized `ActivityNotFoundException` error handling with fallback user notifications on devices missing document providers or file managers.
- Added static release versioning architecture using `gradle.properties` as the single source of truth for `APP_VERSION_CODE` and `APP_VERSION_NAME`.

### Fixed
- Fixed potential application crashes caused by unprotected Storage Access Framework (SAF) calls when no `DocumentsUI` or compatible document provider activity is available.
- Hardened image cropping flow (`CropHelper.kt`) against missing target activity exceptions.
- Synchronized F-Droid metadata, GitHub Actions workflows, and Play Store build properties.

### Changed
- Preserved Git release tags permanently in GitHub Action workflows to support F-Droid build reproducibility.

---

## [1.3.175] - 2026-05-07

### Added
- Introduced dedicated `fdroid` product flavor maintaining 100% open-source software (FOSS) compliance.
- Integrated Tesseract OCR (`tesseract4android`) for offline text extraction in F-Droid and OpenSource flavors.
- Added feature flavor switches in `BuildConfig` for ML Kit vs. Tesseract OCR.

---

## [1.0.0] - 2026-01-15

### Added
- Initial public release of K-PDF (formerly PDF Toolkit).
- Core PDF tools: Merge, Split, Compress, Convert to/from Images, Page Rotation, Watermarking, and Password Protection.
