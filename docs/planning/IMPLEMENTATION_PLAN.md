# K-PDF Implementation Plan

> **This plan requires user approval before any step is executed.**  
> Each phase ends with a checkpoint — do not proceed to the next without explicit sign-off.

---

## Phase 1 — Critical Foundation

### 1.1 Package Rename

**Goal:** Rename all source packages from `com.yourname.pdftoolkit` to `com.kiphra.kpdf`.

**Files affected:** Every `.kt` file, `AndroidManifest.xml`, `build.gradle.kts`, `metadata/`, `FileProvider` authority, Room DB name.

**Steps:**
1. In `app/build.gradle.kts`: change `applicationId = "com.yourname.pdftoolkit"` → `"com.kiphra.kpdf"`.
2. In Android Studio (or via sed): refactor package name `com.yourname.pdftoolkit` → `com.kiphra.kpdf` across all Kotlin files.
3. Update `AndroidManifest.xml` `package` and `authorities` for FileProvider (`${applicationId}.fileprovider`).
4. Update Room DB name from `"pdf_toolkit_database"` → `"kpdf_database"` with a migration or fresh install.
5. Rename F-Droid metadata: `metadata/com.yourname.pdftoolkit.yml` → `metadata/com.kiphra.kpdf.yml`. Update all fields inside.
6. Update `gradle.properties` if anything references the old ID.

**Acceptance criteria:** `./gradlew :app:assembleFdroidDebug` succeeds without errors; package is `com.kiphra.kpdf`.

**Risk:** Room database migration needed if upgrading existing installs; for first Play Store release a fresh install is acceptable.

---

### 1.2 Kiphra Branding

**Goal:** Replace generic purple Material 3 defaults with K-PDF brand identity.

**Files to create/edit:**
- `app/src/main/java/com/kiphra/kpdf/ui/theme/Color.kt` — brand color tokens (see Architecture Plan §10).
- `app/src/main/java/com/kiphra/kpdf/ui/theme/Theme.kt` — rename `PDFToolkitTheme` → `KPdfTheme`; update static palette.
- `app/src/main/res/mipmap-*/` — replace launcher icon with K-PDF icon.
- `app/src/main/res/drawable/` — splash screen vector.
- `app/src/main/AndroidManifest.xml` — `android:label="K-PDF: PDF Editor & Tools"`.
- `app/src/main/res/values/strings.xml` — update `app_name`.

**Deliverables:**
- Brand icon (Kiphra K + PDF document visual language).
- Adaptive icon (`ic_launcher_foreground.xml` + `ic_launcher_background.xml`).
- Splash screen using `SplashScreen` Jetpack API (not deprecated `windowSplashScreenAnimatedIcon`).

---

### 1.3 AdMob Integration (Deferred to v2 / Post-First Release)

> **User Decision (First Release):** No ads on initial launch to ensure maximum user delight, clean onboarding, and smooth store reviews. AdMob monetization architecture is deferred to v2.0.

- `playstore` flavor will remain ad-free on first release (same ad-free UX as F-Droid / Open Source).
- Architecture and placeholder hooks will be designed cleanly so ads can be introduced smoothly later without refactoring.

---

### 1.4 Privacy Policy & Terms

**Goal:** Satisfy Play Store policy requirement.

**Steps:**
1. Create a hosted privacy policy URL (GitHub Pages or simple HTML).
2. Add `PrivacyPolicyScreen.kt` or a WebView-based `PolicyScreen` with the URL.
3. Add a "Privacy Policy" link in `SettingsScreen.kt`.
4. Reference the URL in Play Store console listing.

---

### 1.5 WorkManager for Long Operations

**Goal:** Prevent OS-kill for large file merge/compress/scan.

**New files:**
- `domain/worker/MergeWorker.kt`
- `domain/worker/CompressWorker.kt`
- `domain/worker/ScanWorker.kt`

**Steps:**
1. Add `implementation("androidx.work:work-runtime-ktx:2.9.x")` to `build.gradle.kts`.
2. Create `CoroutineWorker` for each long operation. Workers receive input URIs via `inputData`, post progress via `setProgress()`, emit result URI in `outputData`.
3. Update `MergeViewModel` and `CompressScreen` ViewModel to dispatch WorkManager when input count > 5 or estimated size > 20 MB; otherwise keep inline coroutine approach.
4. Show persistent notification while worker runs.

---

### 1.6 Hindi Tesseract OCR

**Goal:** OCR support for Hindi documents (critical for India market).

**Steps:**
1. Download `hin.traineddata` from tessdata_fast: https://github.com/tesseract-ocr/tessdata_fast
2. Place in `app/src/fdroid/assets/tessdata/hin.traineddata` (and `opensource` sourceSet).
3. Update `OcrEngine.kt` (fdroid) to accept a `language: String` parameter (default `"eng"`).
4. Add a language picker in `OcrScreen.kt` — "English" / "Hindi" / "English + Hindi" options.
5. For multi-language: pass `"eng+hin"` to `TessBaseAPI.init()`.

**Size impact:** ~15 MB added to fdroid APK. Acceptable; keep total APK under 60 MB.

---

## Phase 2 — Quality & Polish

> Begin after Phase 1 is complete and Play Store submission approved.

### 2.1 OOM Fix — PdfAnnotator / PdfSigner / PdfWatermarker

**Problem:** These three classes call `PDDocument.load(inputStream)` without `MemoryUsageSetting.setupTempFileOnly()`.

**Fix:** Add `MemoryUsageSetting` parameter:
```kotlin
// Before
document = PDDocument.load(inputStream)

// After  
document = PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly())
```

**Files:** `PdfAnnotator.kt`, `PdfSigner.kt`, `PdfWatermarker.kt` (3 occurrences each, in `addAnnotations`, `addSignature`, `addSignatureFromSaved`, `addWatermark`).

---

### 2.2 OCR → Searchable PDF

**Goal:** Embed OCR text as an invisible text layer over the original PDF page.

**Approach (PDFBox):**
1. Run OCR on each page rendered as a bitmap.
2. For each `OcrWord` with `boundingBox`, add invisible text at the equivalent PDF coordinate using `PDPageContentStream` with `setRenderingMode(3)` (invisible text).
3. Scale bounding box from bitmap pixels to PDF user units.

**Files to edit/create:**
- `domain/operations/PdfOcrEmbedder.kt` (new)
- `ui/screens/OcrScreen.kt` — add "Make Searchable PDF" output option.

---

### 2.3 Scanner — 4-Point Perspective Correction (fdroid path)

**Goal:** Manual perspective warp without OpenCV dependency.

**Approach:**
1. After capture, show the image in a fullscreen view with 4 draggable corner handles.
2. On "Apply": compute a perspective transform matrix from the 4 corner points.
3. Apply perspective warp using Android `Matrix` + `Canvas.drawBitmap()` with bilinear filtering.
4. This is a lightweight alternative to OpenCV (~100 lines of math vs 10 MB native lib).

**Files:**
- `util/PerspectiveTransform.kt` (new — matrix math)
- `ui/screens/ScanToPdfScreen.kt` — add crop/perspective step between capture and confirmation.

---

### 2.4 Batch Operations

**Goal:** Apply a single operation (compress, watermark, page-number) to multiple input files at once.

**Approach:**
- Reuse existing operation classes in a loop.
- Dispatch via WorkManager (from Phase 1.5) for reliability.
- Show batch progress in a dedicated `BatchResultScreen`.

---

### 2.5 PDF Viewer — Text Search

**Goal:** Search for text in the currently open PDF.

**Approach:**
1. Use `PDFTextStripper` (from PDFBox) to extract text with position data per page.
2. Store results as a list of `SearchResult(pageIndex, boundingBox)`.
3. Navigate to matching page; draw highlight overlay over the matching position.

**Files to edit:**
- `ui/screens/PdfViewerScreen.kt` — add search bar + result navigation.
- `ui/screens/PdfViewerViewModel.kt` — add `searchText()` suspend fun.

---

## Phase 3 — Advanced Features

> Begin after Phase 2 ships and user feedback informs priorities.

### 3.1 Cryptographic PDF Signing

**Approach:** Use BouncyCastle + PDFBox signature API (`PDSignature`) to embed a self-signed certificate. Walk user through one-time key generation stored in Android Keystore. Outputs a CAdES-B compatible signature.

**Effort:** High. Requires careful testing with Adobe Acrobat validation.

### 3.2 Downloadable Tesseract Language Packs

**Approach:** In-app download manager (using `DownloadManager`) for additional `.traineddata` files. Store in `filesDir/tessdata/`. UI: "More Languages" button in OCR settings.

### 3.3 Digital Clipboard / Recent Operations

**Goal:** Show a history of recent operations (merge used these 3 files, compress saved X KB).

**Approach:** Add `OperationLogEntity` to Room. Log each completed operation with timestamp, type, file names, bytes saved. Display in a dedicated "History" tab or section of FilesScreen.

---

## Implementation Order Summary

```
Phase 1 (Pre-Launch v1.0 - Ad-Free Clean Launch):
  1.1 Package rename          ← Do FIRST; affects everything else
  1.2 Branding & Polish       ← Can parallel with 1.1 after rename
  1.3 Privacy policy          ← Core compliance requirement
  1.4 OOM Memory Safeguards   ← Critical stability fix
  1.5 Hindi Tesseract OCR     ← Essential for India audience
  1.6 WorkManager             ← Background reliability

Phase 2 (Post-Launch v1.1):
  2.1 Searchable PDF OCR      ← High value
  2.2 Scanner perspective     ← High value for India users
  2.3 Batch operations        ← After WorkManager
  2.4 PDF search              ← In-document text search

Phase 3 (v2.0+):
  3.1 Sustainable Monetization (AdMob / Pro IAP)
  3.2 Cryptographic signing (DSC/PKCS#12)
  3.3 Language packs (Gujarati, Marathi, etc.)
  3.4 Operation history & Cloud sync
```

---

## Testing Requirements

| Test | Type | Tool |
|---|---|---|
| Package rename builds | Smoke | `./gradlew assembleAll` |
| PDF merge/split correctness | Unit | Existing `PdfMergeTest`, `PdfSplitTest` |
| OCR word detection | Instrumented | Existing `OcrTest.kt` |
| Annotation rendering | Instrumented | Existing `AnnotationTest.kt` |
| AdMob (not shown in fdroid) | Manual | Flavor comparison |
| Memory under large file | Manual | 100-page, 50MB PDF through each operation |
| Navigation back-stack | Instrumented | Existing `NavigationTest.kt` |
