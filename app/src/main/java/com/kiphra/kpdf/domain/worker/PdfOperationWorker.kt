package com.kiphra.kpdf.domain.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kiphra.kpdf.R
import com.kiphra.kpdf.domain.operations.CompressionLevel
import com.kiphra.kpdf.domain.operations.PdfCompressor
import com.kiphra.kpdf.domain.operations.PdfMerger
import com.kiphra.kpdf.util.OutputFolderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker for heavy PDF operations (Merge, Compress, Batch).
 * Prevents OS-kill when users leave the app during lengthy PDF processing.
 */
class PdfOperationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_OPERATION = "key_operation"
        const val KEY_INPUT_URIS = "key_input_uris"
        const val KEY_OUTPUT_NAME = "key_output_name"
        const val KEY_COMPRESSION_LEVEL = "key_compression_level"
        const val KEY_PROGRESS = "key_progress"
        const val KEY_OUTPUT_URI = "key_output_uri"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val OP_MERGE = "merge"
        const val OP_COMPRESS = "compress"

        private const val NOTIFICATION_CHANNEL_ID = "kpdf_operations"
        private const val NOTIFICATION_ID = 2001

        fun enqueueMerge(
            context: Context,
            inputUris: List<Uri>,
            outputName: String
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_OPERATION to OP_MERGE,
                KEY_INPUT_URIS to inputUris.map { it.toString() }.toTypedArray(),
                KEY_OUTPUT_NAME to outputName
            )
            val request = OneTimeWorkRequestBuilder<PdfOperationWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request
        }

        fun enqueueCompress(
            context: Context,
            inputUri: Uri,
            outputName: String,
            level: CompressionLevel = CompressionLevel.MEDIUM
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_OPERATION to OP_COMPRESS,
                KEY_INPUT_URIS to arrayOf(inputUri.toString()),
                KEY_OUTPUT_NAME to outputName,
                KEY_COMPRESSION_LEVEL to level.name
            )
            val request = OneTimeWorkRequestBuilder<PdfOperationWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val operation = inputData.getString(KEY_OPERATION) ?: return@withContext Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Missing operation type")
        )
        val uriStrings = inputData.getStringArray(KEY_INPUT_URIS) ?: return@withContext Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Missing input files")
        )
        val outputName = inputData.getString(KEY_OUTPUT_NAME) ?: "kpdf_output.pdf"

        try {
            setForeground(createForegroundInfo(operation, 0))
        } catch (_: Exception) {
            // Foreground service may not be granted in some test environments
        }

        return@withContext when (operation) {
            OP_MERGE -> runMerge(uriStrings.map { Uri.parse(it) }, outputName)
            OP_COMPRESS -> {
                val levelName = inputData.getString(KEY_COMPRESSION_LEVEL) ?: CompressionLevel.MEDIUM.name
                val level = try {
                    CompressionLevel.valueOf(levelName)
                } catch (_: Exception) {
                    CompressionLevel.MEDIUM
                }
                runCompress(Uri.parse(uriStrings.first()), outputName, level)
            }
            else -> Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Unknown operation: $operation"))
        }
    }

    private suspend fun runMerge(inputUris: List<Uri>, outputName: String): Result {
        val streamResult = OutputFolderManager.createOutputStream(context, outputName)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Failed to create output file"))

        val merger = PdfMerger()
        return try {
            val mergeResult = streamResult.outputStream.use { os ->
                merger.mergePdfs(
                    context = context,
                    inputUris = inputUris,
                    outputStream = os,
                    onProgress = { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                        try {
                            setForegroundAsync(createForegroundInfo(OP_MERGE, percent))
                        } catch (_: Exception) {}
                    }
                )
            }

            if (mergeResult.isSuccess) {
                Result.success(workDataOf(KEY_OUTPUT_URI to streamResult.outputFile.contentUri.toString()))
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to (mergeResult.exceptionOrNull()?.message ?: "Merge failed")))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Merge exception")))
        }
    }

    private suspend fun runCompress(inputUri: Uri, outputName: String, level: CompressionLevel): Result {
        val streamResult = OutputFolderManager.createOutputStream(context, outputName)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Failed to create output file"))

        val compressor = PdfCompressor()
        return try {
            val compressResult = streamResult.outputStream.use { os ->
                compressor.compressPdf(
                    context = context,
                    inputUri = inputUri,
                    outputStream = os,
                    level = level,
                    onProgress = { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                        try {
                            setForegroundAsync(createForegroundInfo(OP_COMPRESS, percent))
                        } catch (_: Exception) {}
                    }
                )
            }

            if (compressResult.isSuccess) {
                Result.success(workDataOf(KEY_OUTPUT_URI to streamResult.outputFile.contentUri.toString()))
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to (compressResult.exceptionOrNull()?.message ?: "Compression failed")))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Compression exception")))
        }
    }

    private fun createForegroundInfo(operation: String, progress: Int): ForegroundInfo {
        createNotificationChannel()

        val title = when (operation) {
            OP_MERGE -> context.getString(R.string.merge_title)
            OP_COMPRESS -> context.getString(R.string.compress_title)
            else -> "K-PDF Processing"
        }

        val contentText = "$title... $progress%"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "K-PDF Background Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of ongoing PDF processing"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
