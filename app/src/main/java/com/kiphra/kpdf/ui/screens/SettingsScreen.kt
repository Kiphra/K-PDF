package com.kiphra.kpdf.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiphra.kpdf.ui.theme.JutCrimsonPrimary
import com.kiphra.kpdf.ui.theme.JutDarkBorderColor
import com.kiphra.kpdf.ui.theme.JutDarkTextMuted
import com.kiphra.kpdf.BuildConfig
import com.kiphra.kpdf.ui.components.LicensesDialog
import com.kiphra.kpdf.util.CacheManager
import com.kiphra.kpdf.util.ThemeManager
import com.kiphra.kpdf.util.ThemeMode
import com.kiphra.kpdf.util.PdfTools
import com.kiphra.kpdf.util.LanguageManager
import com.kiphra.kpdf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Image format options for default setting.
 */
enum class DefaultImageFormat(val displayName: String, val extension: String) {
    WEBP("WebP (Recommended)", "webp"),
    JPEG("JPEG", "jpg")
}

/**
 * Settings preferences manager.
 */
object SettingsPreferences {
    private const val PREFS_NAME = "pdf_toolkit_settings"
    private const val KEY_COMPRESSION_QUALITY = "compression_quality"
    private const val KEY_IMAGE_FORMAT = "default_image_format"
    
    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getCompressionQuality(context: Context): Int {
        return getPrefs(context).getInt(KEY_COMPRESSION_QUALITY, 75)
    }
    
    fun setCompressionQuality(context: Context, quality: Int) {
        getPrefs(context).edit().putInt(KEY_COMPRESSION_QUALITY, quality).apply()
    }
    
    fun getDefaultImageFormat(context: Context): DefaultImageFormat {
        val formatName = getPrefs(context).getString(KEY_IMAGE_FORMAT, DefaultImageFormat.WEBP.name)
        return try {
            DefaultImageFormat.valueOf(formatName ?: DefaultImageFormat.WEBP.name)
        } catch (e: Exception) {
            DefaultImageFormat.WEBP
        }
    }
    
    fun setDefaultImageFormat(context: Context, format: DefaultImageFormat) {
        getPrefs(context).edit().putString(KEY_IMAGE_FORMAT, format.name).apply()
    }
}

/**
 * Comprehensive Settings Screen with organized sections.
 * Includes: Default compression quality, Default image format, Cache cleanup, About/Privacy/License
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var cacheSize by remember { mutableStateOf("Calculating...") }
    var isClearing by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showFeatureRequestDialog by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var showImageFormatDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageScreen by remember { mutableStateOf(false) }
    
    // Settings state
    var compressionQuality by remember { mutableStateOf(SettingsPreferences.getCompressionQuality(context)) }
    var defaultImageFormat by remember { mutableStateOf(SettingsPreferences.getDefaultImageFormat(context)) }
    
    // Theme state
    val currentTheme by ThemeManager.getThemeMode(context).collectAsState(initial = ThemeMode.SYSTEM)
    
    // Language state
    val currentLanguage by LanguageManager.getLanguageFlow(context).collectAsState(initial = LanguageManager.getCurrentLanguage())
    
    // Calculate cache size on screen load
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSize = CacheManager.getFormattedCacheSize(context)
        }
    }
    
    if (showLanguageScreen) {
        LanguageSelectionScreen(
            currentLanguage = currentLanguage,
            onNavigateBack = { showLanguageScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = JutDarkBorderColor.copy(alpha = 0.6f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Quality Settings Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_quality))
            }
            
            // Default Compression Quality
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = stringResource(R.string.cd_compression_quality),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_compression_quality),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${compressionQuality}% - ${getQualityDescription(compressionQuality)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = compressionQuality.toFloat(),
                        onValueChange = { 
                            compressionQuality = it.toInt()
                        },
                        onValueChangeFinished = {
                            SettingsPreferences.setCompressionQuality(context, compressionQuality)
                        },
                        valueRange = 30f..100f,
                        steps = 6,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.compress_smaller_file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.compress_better_quality),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Default Image Format
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_image_format),
                    subtitle = defaultImageFormat.displayName,
                    icon = Icons.Default.Image,
                    onClick = { showImageFormatDialog = true }
                )
            }
            
            // Appearance Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
            }
            
            // Theme Mode
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_theme_mode),
                    subtitle = currentTheme.displayName,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }
            
            // Language Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_language))
            }
            
            // Language Selector
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_select_language),
                    subtitle = LanguageManager.getLanguageDisplayName(currentLanguage),
                    icon = Icons.Default.Language,
                    onClick = { showLanguageScreen = true }
                )
            }
            
            // Storage Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_storage))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_cache_size),
                    subtitle = cacheSize,
                    icon = Icons.Default.Storage,
                    onClick = { showClearCacheDialog = true }
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(onClick = { showClearCacheDialog = true }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }
            
            // Support Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_support))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_request_feature),
                    subtitle = stringResource(R.string.settings_request_feature_subtitle),
                    icon = Icons.Default.Lightbulb,
                    onClick = { showFeatureRequestDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_report_bug),
                    subtitle = stringResource(R.string.settings_report_bug_subtitle),
                    icon = Icons.Default.BugReport,
                    onClick = {
                        showBugReportDialog = true
                    }
                )
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_rate_app_subtitle),
                    icon = Icons.Default.Star,
                    onClick = {
                        openPlayStore(context)
                    }
                )
            }
            
            // About Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_version),
                    subtitle = "v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_licenses),
                    subtitle = stringResource(R.string.settings_licenses_subtitle),
                    icon = Icons.Default.Description,
                    onClick = {
                        showLicensesDialog = true
                    }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_privacy_policy),
                    subtitle = "Document security & privacy policy",
                    icon = Icons.Default.Security,
                    onClick = {
                        showPrivacyPolicyDialog = true
                    }
                )
            }
            
            // App Info Footer
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "K-PDF: PDF Editor & Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} • Offline & Private",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_made_with_love),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Bottom spacing for navigation
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Licenses Dialog
    if (showLicensesDialog) {
        LicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_theme_mode)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        ThemeManager.setThemeMode(context, theme)
                                    }
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == theme,
                                onClick = {
                                    scope.launch {
                                        ThemeManager.setThemeMode(context, theme)
                                    }
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (theme) {
                                        ThemeMode.LIGHT -> "Always use light theme"
                                        ThemeMode.DARK -> "Always use dark theme"
                                        ThemeMode.SYSTEM -> "Follow system settings"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    
    // Image Format Selection Dialog
    if (showImageFormatDialog) {
        AlertDialog(
            onDismissRequest = { showImageFormatDialog = false },
            icon = { Icon(Icons.Default.Image, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_image_format)) },
            text = {
                Column {
                    DefaultImageFormat.entries.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultImageFormat = format
                                    SettingsPreferences.setDefaultImageFormat(context, format)
                                    showImageFormatDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultImageFormat == format,
                                onClick = {
                                    defaultImageFormat = format
                                    SettingsPreferences.setDefaultImageFormat(context, format)
                                    showImageFormatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = format.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (format == DefaultImageFormat.WEBP) {
                                    Text(
                                        text = "Best compression, smaller files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Universal compatibility",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageFormatDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    
    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_clear_cache)) },
            text = {
                Text(stringResource(R.string.settings_clear_cache_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClearing = true
                        showClearCacheDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                CacheManager.clearAllCache(context)
                            }
                            cacheSize = CacheManager.getFormattedCacheSize(context)
                            isClearing = false
                            Toast.makeText(context, context.getString(R.string.settings_cache_cleared), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "K-PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "by Kiphra • PDF Editor & Tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Version info card with hairline border
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, JutDarkBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Build Code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Distribution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${BuildConfig.FLAVOR.replaceFirstChar { it.uppercase() }} (${BuildConfig.BUILD_TYPE})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Platform", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // Features list
                    Text(
                        text = stringResource(R.string.settings_about_features),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    listOf(
                        "• " + stringResource(R.string.settings_about_feature_1),
                        "• " + stringResource(R.string.settings_about_feature_2),
                        "• " + stringResource(R.string.settings_about_feature_3),
                        "• " + stringResource(R.string.settings_about_feature_4),
                        "• " + stringResource(R.string.settings_about_feature_5)
                    ).forEach { feature ->
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val buildInfo = "K-PDF v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}, Flavor: ${BuildConfig.FLAVOR}, Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE})"
                        copyToClipboard(context, "K-PDF Version Info", buildInfo)
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Info")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Document Security & Privacy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pillar 1: On-device
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, JutDarkBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("On-Device Document Processing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "All PDF manipulation, conversions, OCR scanning, and digital signatures run locally on your device. Your documents are never uploaded to remote servers or stored in third-party clouds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pillar 2: Data Confidentiality
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, JutDarkBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Document Confidentiality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "We prioritize your privacy and document safety. K-PDF does not harvest, analyze, or transmit your private document contents, files, or sensitive information.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pillar 3: Permissions
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, JutDarkBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Device Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "• Storage: Used only to select your files and save exported PDFs.\n• Camera: Active only when you scan documents using the camera.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pillar 4: Advertising & SDK Isolation
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, JutDarkBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Advertising & Third-Party SDKs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "To support development, non-intrusive ads may be served by trusted partners (e.g. Google AdMob). Ad networks never have access to your PDF documents, files, camera, or OCR text.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Published by Kiphra • Support: kiphraofficial@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { openPrivacyPolicy(context) }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Web Version")
                }
            }
        )
    }
    
    // Feature Request Dialog
    if (showFeatureRequestDialog) {
        FeatureRequestDialog(
            onDismiss = { showFeatureRequestDialog = false },
            onSendEmail = { category, text ->
                sendFeatureRequest(context, category, text)
                showFeatureRequestDialog = false
            }
        )
    }

    // Bug Report Dialog
    if (showBugReportDialog) {
        BugReportDialog(
            onDismiss = { showBugReportDialog = false },
            onSendEmail = { description ->
                sendBugReport(context, description)
                showBugReportDialog = false
            }
        )
    }
}

@Composable
private fun getQualityDescription(quality: Int): String {
    return when {
        quality >= 90 -> stringResource(R.string.compress_quality_maximum)
        quality >= 75 -> stringResource(R.string.compress_quality_high)
        quality >= 60 -> stringResource(R.string.compress_quality_balanced)
        quality >= 45 -> stringResource(R.string.compress_quality_compressed)
        else -> stringResource(R.string.compress_quality_maximum_compression)
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(7.dp)
                    )
                    .border(
                        width = 0.75.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.1).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )
            }
            
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = JutDarkTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureRequestDialog(
    onDismiss: () -> Unit,
    onSendEmail: (String, String) -> Unit
) {
    var featureText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("New Feature") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val categories = listOf("New Feature", "PDF Tool Request", "UI/UX & Design", "Performance", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.feature_request_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help us improve K-PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.feature_request_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.feature_request_category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = featureText,
                    onValueChange = { featureText = it },
                    label = { Text(stringResource(R.string.feature_request_idea)) },
                    placeholder = { Text(stringResource(R.string.feature_request_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendEmail(category, featureText) },
                enabled = featureText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send via Email")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun BugReportDialog(
    onDismiss: () -> Unit,
    onSendEmail: (String) -> Unit
) {
    val context = LocalContext.current
    var bugDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.settings_report_bug),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help us fix issues & crashes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Describe what happened, error message, or reproduction steps:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = bugDescription,
                    onValueChange = { bugDescription = it },
                    placeholder = { Text("e.g. When trying to merge 3 scanned PDFs, the app closed unexpectedly...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    maxLines = 5
                )

                // Diagnostics Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.75.dp, JutDarkBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Diagnostics Included",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "K-PDF Diagnostics", getDiagnosticInfo())
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy diagnostics",
                                    tint = JutDarkTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nv${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.FLAVOR}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendEmail(bugDescription) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Bug Report")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

// Helper functions for intents & actions

private const val DEVELOPER_EMAIL = "kiphraofficial@gmail.com"
private const val PRIVACY_POLICY_URL = "https://kiphra.github.io/K-PDF/"

private fun getDiagnosticInfo(): String {
    return """
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        App Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})
        Flavor: ${BuildConfig.FLAVOR} (${BuildConfig.BUILD_TYPE})
    """.trimIndent()
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to copy", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmailUniversal(context: Context, subject: String, body: String) {
    try {
        val encodedSubject = Uri.encode(subject)
        val encodedBody = Uri.encode(body)
        val mailtoUri = Uri.parse("mailto:$DEVELOPER_EMAIL?subject=$encodedSubject&body=$encodedBody")
        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(intent, "Send Email")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        copyToClipboard(context, "Support Email", "$DEVELOPER_EMAIL\n\n$subject\n\n$body")
        Toast.makeText(
            context,
            "No email app found. Details copied to clipboard for $DEVELOPER_EMAIL",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun sendFeatureRequest(context: Context, category: String, featureText: String) {
    val subject = "[Feature Request] K-PDF ($category)"
    val emailBody = """
        Feature Idea ($category):
        $featureText
        
        ---
        Diagnostics:
        ${getDiagnosticInfo()}
    """.trimIndent()
    sendEmailUniversal(context, subject, emailBody)
}

private fun sendBugReport(context: Context, bugDescription: String) {
    val subject = "[Bug Report] K-PDF v${BuildConfig.VERSION_NAME}"
    val emailBody = """
        Bug Description & Steps:
        ${if (bugDescription.isBlank()) "[Please describe what happened]" else bugDescription}
        
        ---
        Diagnostics:
        ${getDiagnosticInfo()}
    """.trimIndent()
    sendEmailUniversal(context, subject, emailBody)
}

private fun openPlayStore(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kiphra.kpdf"))
        )
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.kiphra.kpdf"))
        )
    }
}

private fun openPrivacyPolicy(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open Privacy Policy", Toast.LENGTH_SHORT).show()
    }
}
