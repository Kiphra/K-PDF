# K-PDF Architecture Plan

---

## 1. Architecture Principles

| Principle | Current | Target |
|---|---|---|
| Pattern | MVVM + Coroutines | MVVM + Coroutines (keep as-is; no rewrite) |
| Navigation | Single NavHost + sealed Screen class | Extend existing; no migration |
| DI | Manual class construction in screens | Add Hilt (lightweight; avoids boilerplate) |
| Threading | `withContext(Dispatchers.IO)` | Keep + add WorkManager for long jobs |
| Storage | Room (recent files) + DataStore (prefs) | Add migration plan for package rename |
| PDF Engine | PDFBox-Android 2.0.27.0 | Keep (best available FOSS Android PDF lib) |

---

## 2. Package Structure (Post-Rename)

```
com.kiphra.kpdf/
├── KPdfApplication.kt                # Application class
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room DB
│   │   ├── RecentFileEntity.kt
│   │   └── RecentFilesDao.kt
│   ├── preferences/
│   │   └── AppPreferences.kt        # DataStore wrapper
│   ├── SafUriManager.kt             # SAF URI permission management
│   └── PersistedFile.kt
├── domain/
│   ├── operations/                  # All PDF/image processing classes
│   │   ├── PdfMerger.kt
│   │   ├── PdfCompressor.kt
│   │   ├── PdfSplitter.kt
│   │   ├── PdfSecurityManager.kt
│   │   ├── PdfAnnotator.kt
│   │   ├── PdfWatermarker.kt
│   │   ├── PdfSigner.kt
│   │   ├── PdfScanner.kt
│   │   └── OfficeConverter.kt
│   ├── model/                       # Domain data classes
│   └── worker/                      # WorkManager Workers (Phase 1.10)
│       ├── MergeWorker.kt
│       ├── CompressWorker.kt
│       └── ScanWorker.kt
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── Screen.kt
│   ├── screens/                     # All 35+ Compose screens
│   ├── theme/
│   │   ├── Color.kt                 # Kiphra brand tokens
│   │   ├── Type.kt
│   │   └── Theme.kt
│   └── components/                  # Shared composables (dialogs, progress, etc.)
├── util/
│   ├── MemoryGuard.kt
│   ├── CropHelper.kt
│   └── FileUtils.kt
└── di/                              # Hilt modules (Phase 1 addition)
    └── AppModule.kt
```

---

## 3. Layer Responsibilities

### 3.1 Data Layer
- **Room `AppDatabase`**: stores `RecentFileEntity` (URI, name, mime, size, lastAccessed).
- **DataStore `AppPreferences`**: theme mode, language, analytics opt-in.
- **`SafUriManager`**: wraps `takePersistableUriPermission` for content:// URIs from SAF pickers.
- **No network calls** (offline first). Exception: future cloud backup (Phase 3.3) would live here.

### 3.2 Domain Layer
- Pure Kotlin classes, no Android framework references where possible.
- Each operation class has a single responsibility (merge, compress, etc.).
- All public methods are `suspend fun` returning `Result<T>` or domain-specific result types.
- Memory protection via `MemoryGuard.checkMemory()` before every operation.
- PDFBox temp-file mode (`MemoryUsageSetting.setupTempFileOnly()`) used for all document loads (gap: add to `PdfAnnotator`, `PdfSigner`, `PdfWatermarker`).

### 3.3 UI Layer
- Jetpack Compose screens with ViewModels exposing `StateFlow<UiState>`.
- File picker uses `ActivityResultContracts.OpenDocument()` (SAF).
- Output uses `ActivityResultContracts.CreateDocument()` (SAF), not direct file writes.
- No business logic in Composables.

---

## 4. Dependency Injection (Proposed)

Add Hilt. Keep it minimal — only inject classes that are shared across ViewModels.

```kotlin
// di/AppModule.kt
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context) =
        AppDatabase.getDatabase(ctx)

    @Provides @Singleton
    fun provideRecentFilesDao(db: AppDatabase) = db.recentFilesDao()

    @Provides @Singleton
    fun provideAppPreferences(@ApplicationContext ctx: Context) =
        AppPreferences(ctx)
}
```

No Hilt is a viable alternative (keep manual construction) if Hilt migration scope is too large for Phase 1.

---

## 5. Background Job Strategy (WorkManager)

For Phase 1.10, wrap long operations in `CoroutineWorker`:

```kotlin
class MergeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // pull URIs from inputData
        // call PdfMerger.merge(...)
        // post progress via setProgress()
        // notify via NotificationManager
        return Result.success()
    }
}
```

**Trigger:** When file count > 5 or estimated output size > 20 MB, offer background processing.  
**Progress:** `setProgress()` → observed by ViewModel via `WorkManager.getWorkInfoByIdLiveData()`.  
**Notification:** Persistent progress notification; completion/failure notification with result URI.

---

## 6. Error Handling

All operation classes return `Result<T>`. UI layer maps failures:

```
OutOfMemoryError         → "File too large. Try splitting first."
IllegalArgumentException → specific field validation message
IOException              → "Could not read/write file. Check storage permissions."
Exception(password)      → "Incorrect password."
Exception(generic)       → "An error occurred: {message}. Please try again."
```

Errors surfaced via Snackbar (transient) or inline error state in ViewModel.

---

## 7. Storage & Privacy

| Item | Decision |
|---|---|
| Output files | Written via SAF `CreateDocument` — user chooses destination |
| Temp files | In `context.cacheDir` — auto-cleared by system on low storage |
| Scan cache | In `context.cacheDir/scans` — cleared by `CacheManager` in Application |
| Signatures | In `context.filesDir/signatures` — not shared externally |
| OCR tessdata | In `context.filesDir/tessdata` — extracted from assets on first use |
| No cloud uploads | ✅ All processing local |
| No analytics | Default; opt-in analytics if added must respect fdroid flavor constraints |

---

## 8. Flavor-Specific Architecture

```
playstore:
  OcrEngine → MLKitOcrEngine (wraps TextRecognition.getClient())
  SmartDocScanner → GMS Document Scanner (IntentSender flow)
  AdManager → AdMob BannerAd + Interstitial (lazy init)
  ReviewManager → Google Play In-App Review

fdroid / opensource:
  OcrEngine → TesseractOcrEngine (Tesseract4Android)
  SmartDocScanner → CameraXScanner (manual capture, no edge detection)
  AdManager → NoOpAdManager (stub)
  ReviewManager → NoOpReviewManager (stub)
```

Each flavor-specific class lives in `src/{flavor}/java/...` and shares the same interface from `src/main/`.

---

## 9. PDF Viewer Architecture

`PdfViewerScreen.kt` (113 KB) + `PdfViewerViewModel.kt` (53 KB) implement a full PDF viewer using Android's native `PdfRenderer`. 

Key design:
- Pages rendered to `Bitmap` on demand via `PdfRenderer.Page.render()`
- Rendered bitmaps cached in memory (LRU-bounded by ViewModel)
- Touch gestures: pinch-to-zoom on rendered bitmap; annotation overlay drawn on `Canvas`
- **Do not break**: multi-touch pinch-to-zoom; `BlendMode.MULTIPLY` highlighter; zoom controls hidden by default

---

## 10. Brand Architecture (New)

```
ui/theme/Color.kt:
  val KPdfPrimary = Color(0xFF1A73E8)         // Kiphra blue
  val KPdfPrimaryDark = Color(0xFF0D47A1)
  val KPdfSecondary = Color(0xFF34A853)        // Kiphra green accent
  val KPdfBackground = Color(0xFFF8F9FA)
  val KPdfSurface = Color(0xFFFFFFFF)
  val KPdfError = Color(0xFFEA4335)
  
  // Dark mode
  val KPdfPrimaryDarkMode = Color(0xFF8AB4F8)
  val KPdfBackgroundDark = Color(0xFF1A1A2E)
```

Dynamic color (Android 12+) remains enabled as the first choice; above palette used as fallback.
