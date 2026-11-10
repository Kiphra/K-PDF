package com.kiphra.kpdf.domain.operations

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Tesseract OCR Engine for F-Droid build.
 * Uses open-source Tesseract OCR library with tessdata_fast English model.
 */
class OcrEngine(private val context: Context) {
    
    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true
            
            val tessDataPath = File(context.filesDir, "tessdata")
            if (!tessDataPath.exists()) {
                tessDataPath.mkdirs()
            }
            
            val models = listOf("eng.traineddata", "hin.traineddata")
            val availableLangs = mutableListOf<String>()
            for (model in models) {
                val targetFile = File(tessDataPath, model)
                try {
                    val assetFd = try { context.assets.openFd("tessdata/$model") } catch (e: Exception) { null }
                    val assetLength = assetFd?.length ?: -1L
                    assetFd?.close()
                    
                    if (!targetFile.exists() || (assetLength > 0 && targetFile.length() != assetLength)) {
                        Log.d(TAG, "Copying tessdata/$model from assets to ${targetFile.absolutePath}")
                        context.assets.open("tessdata/$model").use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (targetFile.exists()) {
                        availableLangs.add(model.removeSuffix(".traineddata"))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Model $model not loaded: ${e.message}")
                }
            }
            
            tessBaseAPI = TessBaseAPI()
            tessBaseAPI?.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            val langString = if (availableLangs.isNotEmpty()) availableLangs.joinToString("+") else "eng"
            var success = tessBaseAPI?.init(context.filesDir.absolutePath, langString) == true
            if (!success && langString != "eng") {
                Log.w(TAG, "Combined init ($langString) failed, falling back to eng")
                success = tessBaseAPI?.init(context.filesDir.absolutePath, "eng") == true
            }
            isInitialized = success
            Log.d(TAG, "TessBaseAPI initialization result: $success with languages $langString")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Tesseract OCR engine", e)
            false
        }
    }
    
    suspend fun recognizeText(bitmap: Bitmap): List<OcrWord> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                val initResult = initialize()
                if (!initResult) {
                    Log.e(TAG, "Failed to initialize TessBaseAPI before recognizeText")
                    return@withContext emptyList()
                }
            }

            // Ensure bitmap is in ARGB_8888 software format for Tesseract C++ engine
            val safeBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888 || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE)) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }

            try {
                tessBaseAPI?.setImage(safeBitmap) ?: return@withContext emptyList()

                // Crucial: calling utF8Text triggers Tesseract recognition analysis before fetching resultIterator
                val fullText = tessBaseAPI?.utF8Text ?: ""
                Log.d(TAG, "Page text length: ${fullText.length}")
                if (fullText.isBlank()) {
                    return@withContext emptyList()
                }

                val iterator = tessBaseAPI?.resultIterator ?: run {
                    Log.w(TAG, "TessBaseAPI resultIterator returned null after utF8Text on image (${safeBitmap.width}x${safeBitmap.height})")
                    return@withContext emptyList()
                }

                val words = mutableListOf<OcrWord>()
                try {
                    do {
                        val text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD) ?: continue
                        if (text.isBlank()) continue
                        val rect = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                        words.add(
                            OcrWord(
                                text = text,
                                boundingBox = rect?.let { OcrBoundingBox(it.left, it.top, it.right, it.bottom) },
                                confidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                            )
                        )
                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
                } finally {
                    iterator.delete()
                }
                Log.d(TAG, "Recognized ${words.size} words from bitmap (${safeBitmap.width}x${safeBitmap.height})")
                words
            } finally {
                if (safeBitmap != bitmap && !safeBitmap.isRecycled) {
                    safeBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing text with Tesseract", e)
            emptyList()
        }
    }
    
    fun close() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "OcrEngine"
    }
}
