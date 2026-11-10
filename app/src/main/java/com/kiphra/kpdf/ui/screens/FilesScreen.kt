package com.kiphra.kpdf.ui.screens
import com.kiphra.kpdf.util.safeLaunch

import androidx.compose.ui.res.stringResource
import com.kiphra.kpdf.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiphra.kpdf.data.FileManager
import com.kiphra.kpdf.data.PersistedFile
import com.kiphra.kpdf.data.SafUriManager
import com.kiphra.kpdf.ui.theme.JutCrimsonPrimary
import com.kiphra.kpdf.ui.theme.JutDarkBorderColor
import com.kiphra.kpdf.ui.theme.JutDarkTextMuted
import com.kiphra.kpdf.ui.theme.JutSkyBlue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * File type filter options.
 */
enum class FileFilter(val title: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Folder),
    PDF("PDF", Icons.Default.PictureAsPdf),
    DOCS("Docs", Icons.Default.Description)
}

/**
 * Files Screen - Content management tab.
 * Purpose: File access, NOT tools.
 * 
 * Features:
 * - Recent files list (PDF, Images)
 * - Open Document button using SAF (ACTION_OPEN_DOCUMENT)
 * - System file picker with persistent URI permissions
 * - Simple filters (PDF / Image)
 * 
 * IMPORTANT: This screen uses SafUriManager for proper SAF compliance.
 * All files are stored as URI strings, NOT file paths.
 * This ensures proper scoped storage compliance on Android 10+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onOpenPdfViewer: (Uri, String) -> Unit = { _, _ -> },
    onOpenDocViewer: (Uri, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(FileFilter.ALL) }
    var recentFiles by remember { mutableStateOf<List<PersistedFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Supported MIME types for document picker (PDF + Word)
    val docMimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword"
    )

    fun isWordFile(name: String, mimeType: String?): Boolean {
        return mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
            mimeType == "application/msword" ||
            name.endsWith(".docx", true) || name.endsWith(".doc", true)
    }
    
    /**
     * Copy content URI to app cache for reliable access.
     * This is critical for in-app picker URIs that lose permission quickly.
     */
    suspend fun copyUriToCache(context: Context, uri: Uri, fileName: String? = null): android.net.Uri? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "viewer_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val ext = fileName?.substringAfterLast('.', "")?.lowercase()
                ?.takeIf { it == "pdf" || it == "docx" || it == "doc" } ?: "pdf"
            val prefix = if (ext == "pdf") "pdf" else "doc"
            val tempFile = File(cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            
            // Try to copy the file
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            
            // Return file:// URI for direct file access
            android.net.Uri.fromFile(tempFile)
        } catch (e: Exception) {
            android.util.Log.e("FilesScreen", "Failed to copy URI to cache", e)
            null
        }
    }
    
    /**
     * Document picker using SAF (ACTION_OPEN_DOCUMENT).
     * Immediately copies picked file to cache before opening to avoid permission issues.
     */
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                // Take persistable URI permission immediately
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val persistedFile = SafUriManager.addRecentFile(context, selectedUri, flags)
                
                if (persistedFile != null) {
                    // Update local list immediately
                    recentFiles = SafUriManager.loadRecentFiles(context)

                    // Open PDF files - copy to cache first for reliable access
                    if (persistedFile.mimeType == "application/pdf") {
                        // CRITICAL: Copy to cache before opening to avoid permission expiration
                        val cachedUri = copyUriToCache(context, selectedUri, persistedFile.name)
                        if (cachedUri != null) {
                            onOpenPdfViewer(cachedUri, persistedFile.name.substringBeforeLast('.'))
                        } else {
                            // Fallback to direct URI if copy fails (may fail on some devices)
                            onOpenPdfViewer(selectedUri, persistedFile.name.substringBeforeLast('.'))
                        }
                    } else if (isWordFile(persistedFile.name, persistedFile.mimeType)) {
                        // CRITICAL: Copy to cache before opening, same as PDFs —
                        // stored Downloads/provider URIs lose their grant after
                        // restart, which otherwise surfaces as Permission Denial.
                        val cachedUri = copyUriToCache(context, selectedUri, persistedFile.name)
                        if (cachedUri != null) {
                            onOpenDocViewer(cachedUri, persistedFile.name)
                        } else {
                            // Fallback to direct URI if copy fails (may fail on some devices)
                            onOpenDocViewer(selectedUri, persistedFile.name)
                        }
                    }
                } else {
                    // Fallback: try to open anyway, may fail if no permission
                    val mimeType = context.contentResolver.getType(selectedUri)
                    val name = getFileName(context, selectedUri)

                    if (mimeType == "application/pdf") {
                        val cachedUri = copyUriToCache(context, selectedUri)
                        if (cachedUri != null) {
                            onOpenPdfViewer(cachedUri, name)
                        } else {
                            onOpenPdfViewer(selectedUri, name)
                        }
                    } else if (isWordFile(name, mimeType)) {
                        onOpenDocViewer(selectedUri, name)
                    }
                }
            }
        }
    }
    
    // Load recent files from SafUriManager
    LaunchedEffect(Unit) {
        isLoading = true
        recentFiles = SafUriManager.loadRecentFiles(context)
        isLoading = false
    }
    
    // Filter files based on selection
    val filteredFiles = remember(recentFiles, selectedFilter) {
        when (selectedFilter) {
            FileFilter.ALL -> recentFiles
            FileFilter.PDF -> recentFiles.filter { it.mimeType == "application/pdf" }
            FileFilter.DOCS -> recentFiles.filter { isWordFile(it.name, it.mimeType) }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Open Document Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = {
                documentPickerLauncher.safeLaunch(docMimeTypes, context)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = JutCrimsonPrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 0.75.dp,
                            color = JutCrimsonPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = JutCrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Open Document",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                letterSpacing = (-0.2).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "SAF // BROWSE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = JutDarkTextMuted.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select any PDF or Word file from storage",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = JutDarkTextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Filter tabs (JUT Connect pill style)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FileFilter.entries) { filter ->
                val selected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) JutCrimsonPrimary else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (selected) JutCrimsonPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else JutDarkTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = filter.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 11.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else JutDarkTextMuted
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Recent Files Section Header with JUT Kicker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "01 // RECENT DOCUMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            if (recentFiles.isNotEmpty()) {
                TextButton(
                    onClick = { showClearHistoryDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.history_clear_all),
                        modifier = Modifier.size(16.dp),
                        tint = JutDarkTextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_clear).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = JutDarkTextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recent files",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open a document to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFiles, key = { it.uriString }) { file ->
                    RecentFileItem(
                        file = file,
                        onClick = {
                            scope.launch {
                                val uri = file.toUri()
                                if (uri != null) {
                                    // Update last accessed time
                                    SafUriManager.updateLastAccessed(context, file.uriString)

                                    // CRITICAL: Copy to cache before opening to avoid permission expiration
                                    val cachedUri = copyUriToCache(context, uri, file.name)
                                    if (cachedUri == null) {
                                        // Source is gone (e.g. cleared cache copy):
                                        // opening it would only show a parse-error
                                        // screen, so explain instead.
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.doc_access_expired),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                    // Route by actual content: some older entries were cached
                                    // under a .pdf name while holding Word bytes (or vice
                                    // versa), which used to land in the wrong viewer.
                                    val displayName = file.name.substringBeforeLast('.')
                                    when (sniffFileKind(cachedUri)) {
                                        FileKind.PDF -> onOpenPdfViewer(cachedUri, displayName)
                                        FileKind.WORD -> onOpenDocViewer(cachedUri, file.name)
                                        FileKind.UNKNOWN -> {
                                            if (file.mimeType == "application/pdf") {
                                                onOpenPdfViewer(cachedUri, displayName)
                                            } else if (isWordFile(file.name, file.mimeType)) {
                                                onOpenDocViewer(cachedUri, file.name)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.doc_access_expired),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
    
    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.files_clear_dialog_title)) },
            text = {
                Text(stringResource(R.string.files_clear_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            SafUriManager.clearAllRecentFiles(context)
                            recentFiles = emptyList()
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private enum class FileKind { PDF, WORD, UNKNOWN }

private fun sniffFileKind(uri: android.net.Uri): FileKind {
    return try {
        val path = uri.path ?: return FileKind.UNKNOWN
        val file = File(path)
        if (!file.exists()) return FileKind.UNKNOWN
        FileInputStream(file).use { input ->
            val header = ByteArray(5)
            val read = input.read(header)
            if (read < 4) return FileKind.UNKNOWN
            if (header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
            ) {
                return FileKind.PDF
            }
            if (header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
                return FileKind.WORD
            }
            if (read >= 4 && header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
                header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()
            ) {
                return FileKind.WORD
            }
            FileKind.UNKNOWN
        }
    } catch (_: Exception) {
        FileKind.UNKNOWN
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentFileItem(
    file: PersistedFile,
    onClick: () -> Unit
) {
    val isPdf = file.mimeType == "application/pdf"
    val badgeColor = if (isPdf) JutCrimsonPrimary else JutSkyBlue
    val badgeText = if (isPdf) "PDF" else "DOC"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon in tinted JUT box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = badgeColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(7.dp)
                    )
                    .border(
                        width = 0.75.dp,
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(file.mimeType),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.substringBeforeLast('.'),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.1).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    )
                    Text(
                        text = "//",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = FileManager.formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "//",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = formatDate(file.lastAccessed).uppercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = JutDarkTextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun getFileIconColor(mimeType: String): androidx.compose.ui.graphics.Color {
    return when {
        mimeType == "application/pdf" -> MaterialTheme.colorScheme.error
        mimeType.startsWith("image/") -> MaterialTheme.colorScheme.tertiary
        mimeType.contains("word") || mimeType.contains("document") -> 
            MaterialTheme.colorScheme.primary
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> 
            MaterialTheme.colorScheme.secondary
        mimeType.contains("powerpoint") || mimeType.contains("presentation") -> 
            MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun getFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
        mimeType.startsWith("image/") -> Icons.Default.Photo
        mimeType.contains("word") || mimeType.contains("document") -> Icons.Default.Description
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> Icons.Default.TableView
        mimeType.contains("powerpoint") || mimeType.contains("presentation") -> Icons.Default.Slideshow
        else -> Icons.Default.InsertDriveFile
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = "Document"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)?.substringBeforeLast('.') ?: name
                }
            }
        }
    } catch (e: Exception) {
        name = uri.lastPathSegment ?: "Document"
    }
    return name
}
