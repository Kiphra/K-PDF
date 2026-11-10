# K-PDF Monetization & ASO Plan

---

## 1. Monetization Strategy

### 1.1 Philosophy

- **Free to use, sustainable via ads** — not freemium/paywalled.
- Ads appear only in the `playstore` flavor.
- `fdroid` and `opensource` builds are always 100% ad-free.
- Never interrupt active file operations with an ad.
- Reward users for high engagement (not punishment for low engagement).

### 1.2 AdMob Ad Unit Strategy

| Ad Unit | Placement | Trigger |
|---|---|---|
| **Banner** | Bottom of ToolsScreen (home tab) | Always visible on home; hidden during active operations |
| **Interstitial** | Between tool result and next action | After every 3rd successful operation; max 1 per 5 min |
| **Rewarded** | "OCR Hindi" / "Batch mode" unlock | Shown only when user taps a rewarded feature button |

### 1.3 Ad Unit IDs

> Replace test IDs with production IDs before release. Store in `gradle.properties` or a secrets file (not committed).

```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_BANNER_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB
ADMOB_INTERSTITIAL_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/IIIIIIIIII
ADMOB_REWARDED_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/RRRRRRRRRR
```

### 1.4 Revenue Projection (India Market, Rough)

| Metric | Estimate |
|---|---|
| eCPM India (banner) | $0.10 – $0.30 |
| eCPM India (interstitial) | $0.80 – $1.50 |
| DAU breakeven (1000 DAU, 2 interstitials/session) | ~$5–$10/day |
| Target DAU for sustainable revenue | 10,000+ |

The India market has lower eCPM than US/EU, but volume compensates. PDF tool apps have strong retention due to utility nature.

### 1.5 Future Monetization Options (Phase 3+)

- **K-PDF Pro** in-app purchase: unlimited batch processing, no ads, cloud backup.
- **Language pack IAP**: premium Tesseract language bundles (legal, medical).
- Do not pursue subscription model for a utility app — one-time IAP performs better in India.

---

## 2. App Store Optimization (ASO)

### 2.1 Play Store Listing

**App Name:** `K-PDF: PDF Editor & Tools`

**Short Description (80 chars):**
```
Merge, split, compress, sign & edit PDFs — offline, fast & private.
```

**Long Description (4000 chars limit — below is a draft):**
```
K-PDF is a powerful, offline PDF toolkit built for India.
No login. No cloud uploads. Your documents stay on your device.

📄 PDF TOOLS
• Merge multiple PDFs into one
• Split PDF by pages, ranges, or every N pages
• Compress PDF to reduce file size (target size mode)
• Reorder, rotate, and delete pages
• Extract specific pages into new PDFs

🔄 CONVERT
• Images to PDF (JPG, PNG, WEBP)
• PDF to Images
• Word (DOCX) to PDF — offline
• Excel (XLSX) to PDF — offline
• HTML / web page to PDF
• Scan document to PDF using camera

🔒 SECURITY
• Lock PDFs with password (AES-256)
• Unlock / remove PDF password
• Add watermarks (text or image)
• Sign PDF with your signature
• Fill PDF forms
• Flatten PDFs

✏️ EDIT & ANNOTATE
• Highlight, underline, strikethrough
• Draw on PDFs (freehand)
• Add sticky notes and text boxes
• Add shapes and stamps

🔍 OCR & TEXT
• Extract text from PDFs
• OCR scanned documents (English + Hindi)
• Make searchable PDFs

🛡️ PRIVACY FIRST
• 100% offline processing
• No account required
• No files uploaded to any server
• No cloud dependency

Available on F-Droid (open source, no ads, no trackers).
```

**Category:** Productivity  
**Content Rating:** Everyone  
**Tags/Keywords:** pdf editor, pdf merger, pdf compressor, pdf tools, pdf scanner, sign pdf, ocr pdf, word to pdf, offline pdf editor India

### 2.2 ASO Keyword Strategy

**Primary keywords (high volume):**
- pdf editor android
- merge pdf
- compress pdf
- pdf to word
- sign pdf

**Secondary (long-tail, India-specific):**
- pdf tools offline india
- pdf scanner app hindi
- pdf compressor free
- password protect pdf android
- ocr hindi english pdf

**Localized keywords for Hindi listing:**
- पीडीएफ एडिटर
- पीडीएफ मर्ज करें
- पीडीएफ कम्प्रेस

### 2.3 Screenshots (Required)

Minimum 2, recommended 8 screenshots. Suggested shots:

1. **Home / Tools screen** — show tool grid with categories
2. **PDF Viewer** — open PDF with annotation toolbar
3. **Merge tool** — drag-and-drop file list
4. **Scanner** — camera live preview + scanned page thumbnails
5. **OCR result** — recognized text with confidence overlay
6. **Security** — lock screen with AES-256 indicator
7. **Compress result** — before/after size comparison
8. **Settings** — language, theme, privacy options

**Feature Graphic (1024x500):** K-PDF logo + tagline "Edit PDFs. Offline. Private."

### 2.4 F-Droid Listing

The in-repo `metadata/com.kiphra.kpdf.yml` (after rename) controls F-Droid app page.

```yaml
AutoName: K-PDF
Categories:
  - Reading
  - Writing
License: Apache-2.0
AuthorName: Kiphra
SourceCode: https://github.com/Kiphra/K-PDF
IssueTracker: https://github.com/Kiphra/K-PDF/issues

Description: |-
  K-PDF is a comprehensive, offline-first PDF toolkit.
  
  No login. No cloud. 100% free and open source.
  
  Features: Merge, Split, Compress, Rotate, Annotate, Sign, OCR (Tesseract),
  Scan to PDF, Word/Excel to PDF, Password Protect, Watermark, Fill Forms.
  
  F-Droid build uses Tesseract OCR (open source) instead of Google ML Kit.
```

---

## 3. India Market Strategy

| Insight | Action |
|---|---|
| India dominates global PDF tool app downloads | Prioritize Hindi UI + Hindi OCR |
| Budget device constraints (2–4 GB RAM, slow storage) | MemoryGuard + temp-file mode (already in place) |
| Students / professionals use PDF for forms & certificates | Form fill and signature are top features |
| Low trust in cloud document apps | Emphasize "No uploads, 100% offline" in all copy |
| F-Droid community growing in India | Maintain fdroid flavor as first-class citizen |

---

## 4. Competitor Differentiation

| Competitor | Weakness | K-PDF Advantage |
|---|---|---|
| Adobe Acrobat Reader | Freemium paywall; subscription | Free; all tools included |
| PDF Extra | Cloud-dependent | 100% offline |
| Smallpdf (web) | Web-only; uploads files | Native Android; no upload |
| IlovePDF | Web; no Hindi OCR | Hindi Tesseract OCR |
| PDF Converter (generic) | No annotation; no sign | Full annotation + signing |

**Key USP sentence for all marketing:** _"Full PDF toolkit — offline, private, no subscription."_
