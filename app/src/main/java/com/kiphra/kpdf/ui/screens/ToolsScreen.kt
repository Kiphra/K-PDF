package com.kiphra.kpdf.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiphra.kpdf.BuildConfig
import com.kiphra.kpdf.R
import com.kiphra.kpdf.data.SafUriManager
import com.kiphra.kpdf.ui.navigation.Screen
import com.kiphra.kpdf.ui.theme.JutCrimsonDeep
import com.kiphra.kpdf.ui.theme.JutEmeraldOnline
import com.kiphra.kpdf.util.safeLaunch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Tool section enumeration for categorization with JUT Connect technical kickers.
 */
enum class ToolSection(val kicker: String, val title: String) {
    QUICK_ACTIONS("01 // ACADEMIC & QUICK", "Quick Actions"),
    ORGANIZE("02 // ORGANIZE & ARRANGE", "Organize & Edit"),
    CONVERT("03 // CONVERSION STUDIO", "Convert & Scan"),
    SECURITY("04 // PRIVACY & VAULT", "Security & Signature"),
    IMAGE_TOOLS("05 // IMAGE STUDIO", "Image Utilities"),
    VIEW_EXPORT("06 // DOCUMENT VIEWER", "View & Export")
}

data class ToolSectionData(val title: String)

@Composable
fun getToolSections(): List<ToolSectionData> {
    return listOf(
        ToolSectionData(stringResource(R.string.category_quick_actions)),
        ToolSectionData(stringResource(R.string.category_organize)),
        ToolSectionData(stringResource(R.string.category_convert)),
        ToolSectionData(stringResource(R.string.category_security)),
        ToolSectionData(stringResource(R.string.category_image_tools)),
        ToolSectionData(stringResource(R.string.category_view_export))
    )
}

private const val FAVORITE_TOOLS_PREFS = "favorite_tools_prefs"
private const val FAVORITE_TOOLS_KEY = "favorite_tool_ids"

private fun loadFavoriteToolIds(context: android.content.Context): Set<String> {
    return context.getSharedPreferences(FAVORITE_TOOLS_PREFS, android.content.Context.MODE_PRIVATE)
        .getStringSet(FAVORITE_TOOLS_KEY, emptySet())
        .orEmpty()
}

private fun saveFavoriteToolIds(context: android.content.Context, ids: Set<String>) {
    context.getSharedPreferences(FAVORITE_TOOLS_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putStringSet(FAVORITE_TOOLS_KEY, ids)
        .apply()
}

/**
 * Data class representing a PDF/Image tool with technical shortcode.
 */
data class ToolItem(
    val id: String,
    val code: String,
    val titleResId: Int,
    val descResId: Int,
    val icon: ImageVector,
    val section: ToolSection,
    val screen: Screen
) {
    @Composable
    fun getTitle(): String = stringResource(titleResId)
    
    @Composable
    fun getDescription(): String = stringResource(descResId)
}

/**
 * Category filter pill selection.
 */
enum class CategoryFilter(val label: String, val section: ToolSection?) {
    ALL("ALL", null),
    QUICK("QUICK", ToolSection.QUICK_ACTIONS),
    ORGANIZE("ORGANIZE", ToolSection.ORGANIZE),
    CONVERT("CONVERT", ToolSection.CONVERT),
    SECURITY("SECURITY", ToolSection.SECURITY),
    IMAGES("IMAGES", ToolSection.IMAGE_TOOLS),
    VIEW("VIEW", ToolSection.VIEW_EXPORT)
}

/**
 * Tools Screen - Primary home screen inspired by JUT Connect engineering aesthetics.
 * Uses refined obsidian surfaces, crimson accents, technical kickers, and 2-column cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToScreen: (Screen) -> Unit,
    onNavigateToRoute: ((String) -> Unit)? = null,
    onOpenPdfViewer: (Uri, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    suspend fun copyUriToCache(context: android.content.Context, uri: Uri): Uri? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "viewer_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "pdf_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            android.util.Log.e("ToolsScreen", "Failed to copy URI to cache", e)
            null
        }
    }
    
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val persistedFile = SafUriManager.addRecentFile(context, selectedUri, flags)
                val name = persistedFile?.name?.substringBeforeLast('.') ?: run {
                    var displayName = "PDF Document"
                    context.contentResolver.query(selectedUri, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                displayName = c.getString(nameIndex)?.substringBeforeLast('.') ?: displayName
                            }
                        }
                    }
                    displayName
                }
                val cachedUri = copyUriToCache(context, selectedUri)
                if (cachedUri != null) {
                    onOpenPdfViewer(cachedUri, name)
                } else {
                    onOpenPdfViewer(selectedUri, name)
                }
            }
        }
    }
    
    val allTools = getAllTools()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }
    var favoriteIds by remember { mutableStateOf(loadFavoriteToolIds(context)) }
    var showFavoritePicker by remember { mutableStateOf(false) }

    fun toggleFavorite(tool: ToolItem) {
        val updated = if (favoriteIds.contains(tool.id)) favoriteIds - tool.id else favoriteIds + tool.id
        favoriteIds = updated
        saveFavoriteToolIds(context, updated)
    }

    fun handleToolClick(tool: ToolItem) {
        if (tool.screen == Screen.Home && tool.id == "view_pdf") {
            pdfPickerLauncher.safeLaunch(arrayOf("application/pdf"), context)
        } else {
            val imageToolIds = listOf("image_compress", "image_resize", "image_convert", "image_metadata")
            if (imageToolIds.contains(tool.id) && onNavigateToRoute != null) {
                val route = Screen.getRouteForToolId(tool.id)
                onNavigateToRoute(route)
            } else {
                onNavigateToScreen(tool.screen)
            }
        }
    }

    val trimmedQuery = searchQuery.trim()
    val searchResults by remember(trimmedQuery) {
        derivedStateOf {
            if (trimmedQuery.isEmpty()) emptyList()
            else {
                allTools.filter { tool ->
                    context.getString(tool.titleResId).contains(trimmedQuery, ignoreCase = true) ||
                        context.getString(tool.descResId).contains(trimmedQuery, ignoreCase = true) ||
                        tool.code.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }
    }

    val favoriteTools by remember(allTools, favoriteIds) {
        derivedStateOf { allTools.filter { favoriteIds.contains(it.id) } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // JUT CONNECT STYLE IDENTITY / ENGINE BANNER
        item {
            EngineIdentityCard()
        }

        // UNIVERSAL SEARCH BAR
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search 20+ PDF & Image tools...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.5.sp
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // CATEGORY PILL FILTER BAR
        if (trimmedQuery.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryFilter.entries.forEach { filter ->
                        val isSelected = selectedCategory == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategory = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.8.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // CONTENT AREA: SEARCH RESULTS OR CATEGORIZED SECTIONS
        if (trimmedQuery.isNotEmpty()) {
            item {
                JutSectionHeader(
                    kicker = "00 // QUERY MATCHES",
                    title = "Search Results (${searchResults.size})"
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.tools_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                item {
                    ToolActionGrid(
                        tools = searchResults,
                        onToolClick = ::handleToolClick
                    )
                }
            }
        } else {
            // FAVORITES SECTION (If exists)
            if (favoriteTools.isNotEmpty() && (selectedCategory == CategoryFilter.ALL || selectedCategory == CategoryFilter.QUICK)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JutSectionHeader(
                            kicker = "00 // PINNED TOOLS",
                            title = stringResource(R.string.tools_favorites)
                        )
                        IconButton(onClick = { showFavoritePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.tools_add_favorite),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    ToolActionGrid(
                        tools = favoriteTools,
                        onToolClick = ::handleToolClick
                    )
                }
            }

            // TOOL SECTIONS
            ToolSection.entries.forEach { section ->
                val matchesCategory = selectedCategory == CategoryFilter.ALL || selectedCategory.section == section
                val sectionTools = allTools.filter { it.section == section }

                if (matchesCategory && sectionTools.isNotEmpty()) {
                    item {
                        JutSectionHeader(
                            kicker = section.kicker,
                            title = section.title
                        )
                    }

                    item {
                        ToolActionGrid(
                            tools = sectionTools,
                            onToolClick = ::handleToolClick
                        )
                    }
                }
            }
        }
    }

    if (showFavoritePicker) {
        FavoritePickerDialog(
            allTools = allTools,
            favoriteIds = favoriteIds,
            onToggleFavorite = ::toggleFavorite,
            onDismiss = { showFavoritePicker = false }
        )
    }
}

/**
 * JUT Connect Inspired Identity & Engine Status Card.
 */
@Composable
private fun EngineIdentityCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section kicker with emerald pulsing dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(JutEmeraldOnline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "00 // OFFLINE SUITE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Security pill badge
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 0.75.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "100% ON-DEVICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "Privacy-first offline document manipulation. No cloud leaks, no subscriptions.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Technical feature tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TechnicalTag(text = "ON-DEVICE")
                TechnicalTag(text = "ML KIT OCR")
                TechnicalTag(text = "FAST ENGINE")
            }
        }
    }
}

@Composable
private fun TechnicalTag(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        )
    }
}

/**
 * Section Header with JUT Connect Kicker + Title.
 */
@Composable
private fun JutSectionHeader(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = kicker.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

/**
 * 2-Column Action Card Grid inspired by JUT Connect _buildActionCard.
 */
@Composable
private fun ToolActionGrid(
    tools: List<ToolItem>,
    onToolClick: (ToolItem) -> Unit
) {
    val rows = tools.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTools.forEach { tool ->
                    JutActionCard(
                        tool = tool,
                        onClick = { onToolClick(tool) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTools.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Individual JUT Connect Style Action Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JutActionCard(
    tool: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(98.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon Container + Technical Shortcode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tinted Icon Box
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 0.75.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.getTitle(),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Micro technical code
                Text(
                    text = tool.code,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                )
            }

            // Bottom Column: Title + Description
            Column {
                Text(
                    text = tool.getTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.getDescription(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Picker dialog for favorite tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritePickerDialog(
    allTools: List<ToolItem>,
    favoriteIds: Set<String>,
    onToggleFavorite: (ToolItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tools_favorites),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolSection.entries.forEach { section ->
                    val sectionTools = allTools.filter { it.section == section }
                    if (sectionTools.isNotEmpty()) {
                        item(key = "header_${section.name}") {
                            Text(
                                text = section.kicker,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        items(sectionTools, key = { it.id }) { tool ->
                            val isFavorite = favoriteIds.contains(tool.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleFavorite(tool) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isFavorite,
                                    onCheckedChange = { onToggleFavorite(tool) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tool.getTitle(),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = tool.getDescription(),
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    )
}

/**
 * Complete list of 25+ tools mapped with technical shortcodes.
 */
@Composable
fun getAllTools(): List<ToolItem> = listOf(
    // 01 // QUICK ACTIONS
    ToolItem(
        id = "merge",
        code = "MERGE",
        titleResId = R.string.tool_merge_pdf,
        descResId = R.string.desc_merge_pdfs,
        icon = Icons.Default.MergeType,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.Merge
    ),
    ToolItem(
        id = "split",
        code = "SPLIT",
        titleResId = R.string.tool_split_pdf,
        descResId = R.string.desc_split_pdf,
        icon = Icons.Default.CallSplit,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.Split
    ),
    ToolItem(
        id = "compress",
        code = "COMP",
        titleResId = R.string.tool_compress_pdf,
        descResId = R.string.desc_compress_pdf,
        icon = Icons.Default.Compress,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.Compress
    ),
    ToolItem(
        id = "scan_to_pdf",
        code = "SCAN",
        titleResId = R.string.tool_scan_to_pdf,
        descResId = R.string.desc_scan_to_pdf,
        icon = Icons.Default.CameraAlt,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.ScanToPdf
    ),
    ToolItem(
        id = "pdf_to_image",
        code = "P2IMG",
        titleResId = R.string.tool_pdf_to_images,
        descResId = R.string.desc_pdf_to_images,
        icon = Icons.Default.PhotoLibrary,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.PdfToImage
    ),
    ToolItem(
        id = "image_to_pdf",
        code = "IMG2P",
        titleResId = R.string.tool_images_to_pdf,
        descResId = R.string.desc_images_to_pdf,
        icon = Icons.Default.Image,
        section = ToolSection.QUICK_ACTIONS,
        screen = Screen.Convert
    ),

    // 02 // ORGANIZE & EDIT
    ToolItem(
        id = "reorder",
        code = "ORDER",
        titleResId = R.string.tool_reorder_pages,
        descResId = R.string.desc_reorder_pages,
        icon = Icons.Default.SwapVert,
        section = ToolSection.ORGANIZE,
        screen = Screen.Reorder
    ),
    ToolItem(
        id = "rotate",
        code = "ROT",
        titleResId = R.string.tool_rotate_pages,
        descResId = R.string.desc_rotate_pages,
        icon = Icons.Default.RotateRight,
        section = ToolSection.ORGANIZE,
        screen = Screen.Rotate
    ),
    ToolItem(
        id = "delete_pages",
        code = "DEL",
        titleResId = R.string.tool_delete_pages,
        descResId = R.string.desc_delete_pages,
        icon = Icons.Default.Delete,
        section = ToolSection.ORGANIZE,
        screen = Screen.Organize
    ),
    ToolItem(
        id = "extract",
        code = "EXTR",
        titleResId = R.string.tool_extract_pages,
        descResId = R.string.desc_extract_pages,
        icon = Icons.Default.ContentCopy,
        section = ToolSection.ORGANIZE,
        screen = Screen.Extract
    ),
    ToolItem(
        id = "print_studio",
        code = "PRINT",
        titleResId = R.string.tool_print_studio,
        descResId = R.string.desc_print_studio,
        icon = Icons.Default.Print,
        section = ToolSection.ORGANIZE,
        screen = Screen.PrintStudio
    ),

    // 03 // CONVERT & SCAN
    ToolItem(
        id = "ocr",
        code = "OCR",
        titleResId = R.string.tool_ocr,
        descResId = R.string.desc_ocr,
        icon = Icons.Default.DocumentScanner,
        section = ToolSection.CONVERT,
        screen = Screen.Ocr
    ),
    ToolItem(
        id = "doc_to_pdf",
        code = "DOCX",
        titleResId = R.string.tool_doc_to_pdf,
        descResId = R.string.desc_doc_to_pdf,
        icon = Icons.Default.Description,
        section = ToolSection.CONVERT,
        screen = Screen.DocToPdf
    ),
    ToolItem(
        id = "html_to_pdf",
        code = "HTML",
        titleResId = R.string.tool_html_to_pdf,
        descResId = R.string.desc_html_to_pdf,
        icon = Icons.Default.Language,
        section = ToolSection.CONVERT,
        screen = Screen.HtmlToPdf
    ),
    ToolItem(
        id = "extract_text",
        code = "TXT",
        titleResId = R.string.tool_extract_text,
        descResId = R.string.desc_extract_text,
        icon = Icons.Default.TextFields,
        section = ToolSection.CONVERT,
        screen = Screen.ExtractText
    ),

    // 04 // SECURITY & PRIVACY
    ToolItem(
        id = "lock",
        code = "LOCK",
        titleResId = R.string.tool_lock_pdf,
        descResId = R.string.desc_lock_pdf,
        icon = Icons.Default.Lock,
        section = ToolSection.SECURITY,
        screen = Screen.Security
    ),
    ToolItem(
        id = "unlock",
        code = "UNLOCK",
        titleResId = R.string.tool_unlock_pdf,
        descResId = R.string.desc_unlock_pdf,
        icon = Icons.Default.LockOpen,
        section = ToolSection.SECURITY,
        screen = Screen.Unlock
    ),
    ToolItem(
        id = "watermark",
        code = "WATER",
        titleResId = R.string.tool_add_watermark,
        descResId = R.string.desc_add_watermark,
        icon = Icons.Default.WaterDrop,
        section = ToolSection.SECURITY,
        screen = Screen.Watermark
    ),
    ToolItem(
        id = "sign",
        code = "SIGN",
        titleResId = R.string.tool_sign_pdf,
        descResId = R.string.desc_sign,
        icon = Icons.Default.Draw,
        section = ToolSection.SECURITY,
        screen = Screen.SignPdf
    ),
    ToolItem(
        id = "fill_forms",
        code = "FORM",
        titleResId = R.string.tool_fill_forms,
        descResId = R.string.desc_fill_forms,
        icon = Icons.Default.EditNote,
        section = ToolSection.SECURITY,
        screen = Screen.FillForms
    ),
    ToolItem(
        id = "flatten",
        code = "FLAT",
        titleResId = R.string.tool_flatten_pdf,
        descResId = R.string.desc_flatten_pdf,
        icon = Icons.Default.Layers,
        section = ToolSection.SECURITY,
        screen = Screen.Flatten
    ),

    // 05 // IMAGE STUDIO
    ToolItem(
        id = "image_compress",
        code = "ICOMP",
        titleResId = R.string.tool_image_compress,
        descResId = R.string.desc_compress_image,
        icon = Icons.Default.Compress,
        section = ToolSection.IMAGE_TOOLS,
        screen = Screen.ImageTools
    ),
    ToolItem(
        id = "image_resize",
        code = "RESIZE",
        titleResId = R.string.tool_image_resize,
        descResId = R.string.desc_resize_image,
        icon = Icons.Default.AspectRatio,
        section = ToolSection.IMAGE_TOOLS,
        screen = Screen.ImageTools
    ),
    ToolItem(
        id = "image_convert",
        code = "ICONV",
        titleResId = R.string.tool_image_convert,
        descResId = R.string.desc_convert_format,
        icon = Icons.Default.Transform,
        section = ToolSection.IMAGE_TOOLS,
        screen = Screen.ImageTools
    ),
    ToolItem(
        id = "image_metadata",
        code = "EXIF",
        titleResId = R.string.tool_image_metadata,
        descResId = R.string.desc_strip_metadata,
        icon = Icons.Default.DeleteSweep,
        section = ToolSection.IMAGE_TOOLS,
        screen = Screen.ImageTools
    ),

    // 06 // VIEW & EXPORT
    ToolItem(
        id = "view_pdf",
        code = "VIEW",
        titleResId = R.string.tool_view_pdf,
        descResId = R.string.desc_view_pdf,
        icon = Icons.Default.PictureAsPdf,
        section = ToolSection.VIEW_EXPORT,
        screen = Screen.Home
    ),
    ToolItem(
        id = "page_numbers",
        code = "NUM",
        titleResId = R.string.tool_page_numbers,
        descResId = R.string.desc_page_numbers,
        icon = Icons.Default.FormatListNumbered,
        section = ToolSection.VIEW_EXPORT,
        screen = Screen.PageNumber
    ),
    ToolItem(
        id = "metadata",
        code = "META",
        titleResId = R.string.tool_view_metadata,
        descResId = R.string.desc_view_metadata,
        icon = Icons.Default.Info,
        section = ToolSection.VIEW_EXPORT,
        screen = Screen.Metadata
    )
)
