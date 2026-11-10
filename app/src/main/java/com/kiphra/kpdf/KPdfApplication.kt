package com.kiphra.kpdf

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.kiphra.kpdf.review.ReviewIntegration
import com.kiphra.kpdf.util.CacheManager
import com.kiphra.kpdf.util.ThemeManager
import com.kiphra.kpdf.util.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Application class for K-PDF: PDF Editor & Tools.
 * Initializes PdfBox-Android on startup and manages cache cleanup.
 */
class KPdfApplication : Application() {
    
    companion object {
        private const val TAG = "KPdf"
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PdfBox-Android
        PDFBoxResourceLoader.init(applicationContext)
        
        // Initialize In-App Review system for session tracking
        ReviewIntegration.initialize(this)
        
        // Initialize theme synchronously to avoid flicker
        runBlocking {
            val themeMode = ThemeManager.getThemeMode(applicationContext).first()
            ThemeManager.applyTheme(themeMode)
            Log.d(TAG, "Theme initialized: $themeMode")
        }
        
        // Initialize language (uses AppCompatDelegate for per-app locale)
        LanguageManager.initializeLanguage(applicationContext)
        Log.d(TAG, "Language initialized: ${LanguageManager.getCurrentLanguage()}")
        
        // Auto-clean cache on startup (runs in background)
        applicationScope.launch {
            try {
                // Clean old cache files (older than 24 hours)
                val cleanedBytes = CacheManager.clearOldCache(applicationContext)
                if (cleanedBytes > 0) {
                    Log.d(TAG, "Auto-cleaned ${cleanedBytes / 1024} KB from cache")
                }
                
                // Also clean PDF operation temp files
                CacheManager.clearPdfOperationsCache(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Cache cleanup failed", e)
            }
        }
    }
}