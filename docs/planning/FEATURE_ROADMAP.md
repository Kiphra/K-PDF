# K-PDF Feature Roadmap

> Strategy: **polish existing, don't over-promise**. The existing codebase already has a comprehensive PDF toolkit. The roadmap focuses on rebranding, missing infrastructure, polish, and high-value additions — not rewrites.

---

## Phase 1 — Critical Foundation (Pre-Launch)

These items are **blockers** for any Play Store or F-Droid release under the K-PDF / Kiphra brand.

| # | Feature | Priority | Effort | Flavors | Notes |
|---|---|---|---|---|---|
| 1.1 | **Package rename** `com.yourname.pdftoolkit` → `com.kiphra.kpdf` | 🔴 | Low | All | Rename all packages, update manifest, F-Droid metadata filename |
| 1.2 | **Kiphra branding** — launcher icon, splash screen, color tokens | 🔴 | Medium | All | Replace purple defaults with K-PDF brand palette; Material 3 tokens |
| 1.3 | **AdMob integration** — banner ads + interstitial on output | 🔴 | Medium | playstore | Rewarded ad for premium operations (optional gating) |
| 1.4 | **Privacy policy + terms page** in app | 🔴 | Low | All | Required by Play Store |
| 1.5 | **Play Store listing assets** — icon, screenshots, feature graphic, descriptions | 🔴 | Medium | playstore | ASO-optimized for India (English + Hindi) |
| 1.6 | **In-app Review** polish — trigger at right moment | 🟡 | Low | playstore | `ReviewIntegration` already in Application class |
| 1.7 | **F-Droid metadata update** after package rename | 🔴 | Low | fdroid | Rename `.yml` file, update package fields |
| 1.8 | **Hindi Tesseract model** — `hin.traineddata` in fdroid/opensource assets | 🟡 | Medium | fdroid, opensource | ~15 MB; enables Hindi OCR offline |
| 1.9 | **Crash reporting** — Sentry SDK (FOSS-friendly) or Firebase Crashlytics | 🟡 | Low | All / playstore | Non-invasive, opt-in for fdroid |
| 1.10 | **WorkManager for long operations** — Merge/Compress/Scan | 🟡 | Medium | All | Prevents kill on background; notification progress |

---

## Phase 2 — Quality & Polish (Post-Launch)

These improve user satisfaction and ratings. All feasible within the existing tech stack.

| # | Feature | Priority | Effort | Flavors | Notes |
|---|---|---|---|---|---|
| 2.1 | **PDF Viewer** — bookmark support, text search highlight, page thumbnails sidebar | 🟡 | High | All | PdfViewerScreen.kt is already 113KB; add search overlay |
| 2.2 | **Scanner perspective correction** (fdroid) | 🟡 | High | fdroid, opensource | Implement OpenCV or manual 4-corner crop; GMS scanner handles this for playstore |
| 2.3 | **OCR → Searchable PDF** — embed recognized text as invisible layer | 🟡 | High | All | Critical differentiator; PDFBox supports text overlay |
| 2.4 | **Image annotation** — freehand drawing on PDF pages with undo/redo | 🟡 | Medium | All | AnnotationScreen exists; add undo stack |
| 2.5 | **Batch operations** — apply compress/rotate/watermark to multiple files | 🟡 | Medium | All | Currently single-file operations |
| 2.6 | **Recent files** — file manager with sort/search | 🟡 | Low | All | FilesScreen.kt exists; needs sort/filter |
| 2.7 | **PdfAnnotator OOM fix** — use temp-file backing for annotation load | 🟡 | Low | All | Risk R5 from audit |
| 2.8 | **Multi-language UI** — Hindi, Gujarati, Marathi strings | 🟡 | Medium | All | LanguageSelectionScreen present; strings need translation |
| 2.9 | **Dark mode refinement** — AMOLED dark + custom Kiphra dark palette | 🟢 | Low | All | Static fallback colors are generic |
| 2.10 | **PrintStudio screen** — impose pages for booklet/4-up printing | 🟢 | High | All | Route listed in ToolsScreen; imposition directory exists |
| 2.11 | **Flatten PDF** — rasterize annotations to content | 🟢 | Medium | All | FlattenScreen.kt exists; verify PDFBox implementation |
| 2.12 | **Export to DOCX** — basic text + image export from PDF | 🟢 | High | All | Requires PDFTextStripper + POI; complex |

---

## Phase 3 — Advanced & Differentiating Features

Higher effort or scope; pursue after Phase 1 & 2 establish a stable user base.

| # | Feature | Priority | Effort | Flavors | Notes |
|---|---|---|---|---|---|
| 3.1 | **Digital signature (cryptographic)** — PKCS#7 / CAdES self-signed | 🟡 | High | All | PdfSigner is visual-only today; legal e-signing requires BC provider |
| 3.2 | **PDF Repair** — fix corrupted/truncated PDFs | 🟢 | Medium | All | RepairScreen.kt exists; verify operation |
| 3.3 | **Google Drive / local backup** — export recent files | 🟢 | High | playstore | Needs Drive API + SAF; privacy-sensitive |
| 3.4 | **Page template library** — lined, dotted, graph paper PDF generation | 🟢 | Medium | All | Offline; purely PDFBox |
| 3.5 | **PDF to Word/Excel conversion** — structured extraction | 🟢 | High | All | Very complex; PdfBox text stripper + heuristics |
| 3.6 | **AI-powered document summarization** — on-device or Gemini API | 🟢 | High | playstore | Requires Gemini API key; network |
| 3.7 | **Multi-window / tablet layout** | 🟢 | High | All | Responsive Compose layout for large screens |
| 3.8 | **Additional Tesseract languages** — Tamil, Telugu, Kannada, Malayalam | 🟢 | Low | fdroid | Each ~15–50 MB; downloadable on demand preferred |

---

## Feasibility Notes

- **Offline first**: All Phase 1 and Phase 2 items are fully offline-capable.
- **Phase 3.6** (AI summarization) requires network and Gemini API key — not available offline.
- **Perspective correction** (Phase 2.2) is high effort without OpenCV; a lightweight manual 4-point crop UI with bilinear warp may be a viable alternative.
- **Digital signatures** (Phase 3.1) require the BouncyCastle library (already potentially included via Apache POI) — feasibility depends on PDFBox-Android's signature API support.
- **Tesseract language packs** should be delivered as on-demand asset packs (Play Asset Delivery) or in-app downloads, not bundled, to keep APK size under 100 MB.
