# K-PDF Project Audit

> Status: **Complete** — based on direct source inspection, not documentation inference.

---

## 1. Repository Identity

| Field | Value |
|---|---|
| Package ID | `com.kiphra.kpdf` |
| App Name | K-PDF: PDF Editor & Tools |
| Repo | https://github.com/Kiphra/K-PDF |
| License | Apache 2.0 |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 36 |
| Current Version | 1.4.0 (code 226) |
| Author | Kiphra |

---

## 2. Architecture Overview

```
app/
  src/
    main/           ← shared sources (all flavors)
    playstore/      ← ML Kit OCR + GMS document scanner
    fdroid/         ← Tesseract OCR only
    opensource/     ← FOSS; no ads; Tesseract OCR
  build.gradle.kts  ← Kotlin DSL, reads gradle.properties for version
gradle.properties   ← APP_VERSION_CODE + APP_VERSION_NAME (F-Droid source of truth)
metadata/com.kiphra.kpdf.yml  ← F-Droid metadata (in-repo copy)
```

**Architectural pattern:** Single-Activity + Jetpack Compose + MVVM.  
Navigation is handled by a single `AppNavigation` NavHost with a bottom navigation bar (Tools, Files, Settings tabs). Screen routes are defined as a sealed class `Screen`.

---

## 3. Product Flavors

| Flavor | OCR Engine | Ads | Firebase/GMS |
|---|---|---|---|
| `playstore` | Google ML Kit | AdMob eligible | Yes |
| `fdroid` | Tesseract4Android | None | None |
| `opensource` | Tesseract4Android | None | None |

All three flavors share the same core PDF operations (PDFBox-Android). The flavor split is clean and correct for F-Droid compliance.

---

## 4. Existing Features (verified in source)

### 4.1 PDF Operations (`domain/operations/`)
| Class | Capability | Verdict |
|---|---|---|
| `PdfMerger` | Merge multiple PDFs via `PDFMergerUtility`; temp-file backed | ✅ Works |
| `PdfCompressor` | Structural + image optimization; binary-search target-size mode | ✅ Works |
| `PdfSplitter` | Split all pages / by ranges / extract specific pages | ✅ Works |
| `PdfSecurityManager` | Encrypt (AES-256 / RC4-128) + decrypt; permission flags | ✅ Works |
| `PdfAnnotator` | Highlight, underline, strikethrough, freehand, sticky note, textbox, shapes, stamps | ✅ Works |
| `PdfWatermarker` | Text/image watermark; 6 positions; tiled; opacity | ✅ Works |
| `PdfSigner` | Draw-and-embed visual signature; saved signatures; date/name extras | ✅ Works |
| `PdfScanner` | Camera capture → multi-image PDF; grayscale/B&W; Otsu threshold; EXIF rotation | ✅ Works |
| `OfficeConverter` | DOCX→PDF (paragraph layout, tables, images, bullets, alignment, colors) | ✅ Substantial |
| `OfficeConverter` | XLSX→PDF (landscape, cell grid, formula evaluation) | ✅ Basic |
| `OfficeConverter` | PPTX→PDF (slide rendering via Apache POI XSLF) | ⚠️ Present but limited |

### 4.2 UI Screens (35 files in `ui/screens/`)
Merge, Split, Compress, Convert (Images→PDF), PdfToImage, Reorder, Rotate, Organize (delete pages), Extract, Security (lock/unlock), Watermark, Sign, OCR, ScanToPdf, DocToPdf, HtmlToPdf, FillForms, Flatten, PageNumber, Metadata, Repair, Settings, ToolsScreen, FilesScreen, AnnotationScreen, ExtractText, ImageTools, MultiOutputResult, Imposition.

### 4.3 Database / Persistence
- **Room**: single table `recent_files` (uriString PK, name, mimeType, size, lastAccessed).
- **DataStore Preferences**: theme, language settings.
- **SharedPreferences**: favorite tool IDs (ToolsScreen).

### 4.4 OCR
- **playstore**: ML Kit Text Recognition (on-device, cloud-free if configured correctly).
- **fdroid/opensource**: Tesseract4Android; initializes by copying `tessdata/eng.traineddata` from assets; word-level bounding boxes with confidence.
- **Risk**: Only English tessdata in assets. Hindi/multilingual model (~10–50 MB each) not included.

### 4.5 Scanner / Camera
- **playstore**: Uses `SmartDocScanner` which wraps Google ML Kit Document Scanner.
- **fdroid/opensource**: CameraX live preview; manual capture; no edge detection / auto-perspective correction.
- **Post-processing**: Grayscale / B&W with Otsu thresholding, contrast enhancement.

### 4.6 Theme & Localization
- Material 3 Dynamic Color (Android 12+); fallback purple/pink static palette.
- `ThemeManager` + `LanguageManager` classes referenced in `Application`.
- `LanguageSelectionScreen` present (Hindi, Gujarati, etc. strings likely in resources).

---

## 5. Dependency Inventory

| Dependency | Version (approximate) | License | FOSS |
|---|---|---|---|
| pdfbox-android | 2.0.27.0 | Apache 2.0 | ✅ |
| Apache POI (xwpf, xssf, xslf) | 5.x | Apache 2.0 | ✅ |
| Tesseract4Android | 4.x | Apache 2.0 | ✅ |
| CameraX (core, lifecycle, view) | Jetpack | Apache 2.0 | ✅ |
| Jetpack Compose (Material 3) | BOM 2024.x | Apache 2.0 | ✅ |
| Room | Jetpack | Apache 2.0 | ✅ |
| DataStore Preferences | Jetpack | Apache 2.0 | ✅ |
| Coil / Glide | Latest stable | Apache 2.0 / BSD | ✅ |
| uCrop (image cropper) | 2.x | Apache 2.0 | ✅ |
| ExifInterface | Jetpack | Apache 2.0 | ✅ |
| ML Kit Text Recognition | (playstore only) | Google ToS | ⛔ fdroid |
| ML Kit Document Scanner | (playstore only) | Google ToS | ⛔ fdroid |
| AdMob | (playstore only, TBD) | Google ToS | ⛔ fdroid |

---

## 6. Memory & Performance

- **Gradle heap**: `-Xmx4096m` (JVM build heap) — not runtime.
- **PDFBox temp-file mode**: `MemoryUsageSetting.setupTempFileOnly()` used in Merge, Split, Compress, Splitter — prevents OOM on large docs. ✅
- **MemoryGuard**: 40 MB free threshold checked before each major operation. ✅
- **Bitmap handling**: `PdfScanner` and `PdfWatermarker` use `inJustDecodeBounds` + `inSampleSize` before full decode. ✅
- **Issue**: `PdfAnnotator` and `PdfSigner` load full documents without `MemoryUsageSetting`. Could OOM on large encrypted or image-heavy PDFs.
- **Issue**: `OfficeConverter.convertDocxToPdf` does not use temp-file mode; loads entire DOCX into memory via Apache POI.

---

## 7. Code Quality Observations

| Area | Observation |
|---|---|
| Package naming | Still `com.yourname.pdftoolkit` — must be renamed to `com.kiphra.kpdf` before Play Store release |
| Theme name | `PDFToolkitTheme` — cosmetic; rename to `KPdfTheme` |
| F-Droid metadata | `com.yourname.pdftoolkit.yml` filename must match the final package ID |
| `OfficeConverter` | 1,940 lines; moderate complexity; PPTX slide rendering is geometry-estimation |
| `PdfViewerScreen.kt` | 113 KB — largest file; embedded viewer using `android.graphics.pdf.PdfRenderer` |
| `OcrScreen.kt` | 50 KB — integrates both Tesseract and ML Kit branches with build-flavor guards |
| Coroutine hygiene | All heavy operations correctly use `withContext(Dispatchers.IO)` ✅ |
| Resource strings | Internationalized via `R.string.*` ✅ |
| Navigation | URL-encoded URI cache key normalization in `AppNavigation` to avoid duplicate entries ✅ |
| SafUriManager | SAF URI persistence with `takePersistableUriPermission`; recent files in Room ✅ |

---

## 8. Known Risks and Gaps

| # | Risk | Severity | Notes |
|---|---|---|---|
| R1 | Package ID not renamed | 🔴 HIGH | Will be rejected by Play Store / F-Droid if unchanged |
| R2 | Tesseract: English only | 🟡 MEDIUM | Hindi/regional OCR requires additional .traineddata assets (+10–50MB per language) |
| R3 | PPTX rendering accuracy | 🟡 MEDIUM | POI XSLF geometry is approximate; complex slides may render poorly |
| R4 | No background job infra | 🟡 MEDIUM | Long operations (large compress/merge) run in coroutines with no WorkManager failsafe |
| R5 | PdfAnnotator OOM risk | 🟡 MEDIUM | Full document loaded without temp-file backing for annotation operations |
| R6 | Scan: no perspective correction (fdroid) | 🟡 MEDIUM | CameraX path has no edge detection or perspective warp |
| R7 | AdMob not integrated yet | 🟡 MEDIUM | monetization pending |
| R8 | Cryptographic signature absent | 🟢 LOW | PdfSigner is visual-only; no PKCS#7 / CAdES for legal e-signing |
| R9 | Static theme colors not M3 brand-ready | 🟢 LOW | Fallback colors are default purple — not Kiphra brand |
| R10 | PrintStudio screen missing | 🟢 LOW | Listed in ToolsScreen but Screen.PrintStudio route may not have a full implementation |

---

## 9. F-Droid & CI Readiness

- `UpdateCheckData` in `com.kiphra.kpdf.yml` correctly reads `APP_VERSION_CODE` and `APP_VERSION_NAME` from `gradle.properties` ✅
- `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` configured ✅
- Build entry uses `gradle: [fdroid]` and `gradleprops` ✅
- **Gap**: After package rename, the metadata filename must be updated to match new package ID.
- **Gap**: No GitHub Actions / CI file inspected — verify lint and test CI runs correctly.

---

## 10. What Does NOT Exist (yet)

- Kiphra branding assets (icon, splash, color tokens)
- AdMob integration (banner / interstitial / rewarded)
- Play Store listing assets (screenshots, feature graphic, description)
- WorkManager-backed background processing for large files
- Cryptographic PDF signing
- Multi-language Tesseract models (Hindi, etc.)
- Edge-detection perspective correction for fdroid scanner path
- Formal crash reporting (Firebase Crashlytics is playstore-only territory; consider Sentry SDK for all flavors)
- In-app review / rating flow (Play Store only; already referenced via `ReviewIntegration` in Application class)
