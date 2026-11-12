package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AiEngineMode
import com.example.data.model.BuiltinWallpaper
import com.example.data.model.ConnectionTestStatus
import com.example.data.model.EditorSettings
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperAlignment
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPageTarget
import com.example.data.model.WallpaperScaleMode
import com.example.data.model.WallpaperSourceType
import com.example.data.prefs.ApiKeyState
import com.example.data.prefs.AppPreferencesState
import com.example.data.repo.StorageSummary
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.theme.EditorFontChoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSection(val titleId: String) {
    MENU("Pengaturan"),
    PROFILE("Profil Penulis"),
    APPEARANCE("Tampilan & Tema"),
    WALLPAPER("Wallpaper Aplikasi"),
    EDITOR("Font & Editor"),
    AI_CONFIG("Konfigurasi AI NARA"),
    BACKUP("Backup & Penyimpanan"),
    PRIVACY("Privasi & Keamanan"),
    GUIDE_FAQ("Panduan, FAQ & Tentang")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferencesState,
    apiKeyState: ApiKeyState,
    connectionTestStatus: ConnectionTestStatus,
    connectionTestMessage: String,
    storageSummary: StorageSummary,
    revisionCount: Int,
    initialSection: SettingsSection = SettingsSection.MENU,
    onUpdateProfile: (String, String, Int, String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateEditorSettings: (EditorSettings) -> Unit,
    onUpdateUseSeparateWallpapers: (Boolean) -> Unit,
    onUpdateWallpaper: (WallpaperPageTarget, WallpaperConfig) -> Unit,
    onPickGalleryWallpaper: (WallpaperPageTarget, Uri, WallpaperConfig) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onDeleteApiKey: () -> Unit,
    onTestConnection: (String?) -> Unit,
    onUpdateAiConfig: (AiEngineMode, Boolean, Boolean) -> Unit,
    onRunManualBackup: () -> Unit,
    onRestoreAutoBackup: () -> Unit,
    onExportFullBackup: (Uri) -> Unit,
    onImportFullBackup: (Uri) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onClearAllChats: () -> Unit,
    onUpdatePrivacyHide: (Boolean) -> Unit,
    onRefreshStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSection by remember(initialSection) { mutableStateOf(initialSection) }
    var selectedWallpaperTarget by remember { mutableStateOf(WallpaperPageTarget.GLOBAL) }

    BackHandler(enabled = currentSection != SettingsSection.MENU) {
        currentSection = SettingsSection.MENU
    }

    val activePreviewWallpaper = if (currentSection == SettingsSection.WALLPAPER) {
        preferences.wallpapers[selectedWallpaperTarget] ?: preferences.resolveWallpaperFor(WallpaperPageTarget.GLOBAL)
    } else {
        preferences.resolveWallpaperFor(WallpaperPageTarget.HOME)
    }

    WallpaperBackdrop(config = activePreviewWallpaper, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = activePreviewWallpaper.panelOpacity)
                    ),
                    navigationIcon = {
                        if (currentSection != SettingsSection.MENU) {
                            IconButton(
                                onClick = { currentSection = SettingsSection.MENU },
                                modifier = Modifier.testTag("settings_back_to_menu")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                            }
                        }
                    },
                    title = { Text(currentSection.titleId) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                when (currentSection) {
                    SettingsSection.MENU -> SettingsMenuList(
                        preferences = preferences,
                        apiKeyState = apiKeyState,
                        storageSummary = storageSummary,
                        onSelectSection = { sec ->
                            if (sec == SettingsSection.BACKUP) onRefreshStorage()
                            currentSection = sec
                        },
                        onOpenRecycleBin = onOpenRecycleBin
                    )

                    SettingsSection.PROFILE -> ProfileSettingsPage(
                        preferences = preferences,
                        onSave = onUpdateProfile
                    )

                    SettingsSection.APPEARANCE -> AppearanceSettingsPage(
                        preferences = preferences,
                        onUpdateTheme = onUpdateThemeMode
                    )

                    SettingsSection.WALLPAPER -> WallpaperSettingsPage(
                        preferences = preferences,
                        target = selectedWallpaperTarget,
                        onSelectTarget = { selectedWallpaperTarget = it },
                        onToggleSeparate = onUpdateUseSeparateWallpapers,
                        onUpdateConfig = { cfg -> onUpdateWallpaper(selectedWallpaperTarget, cfg) },
                        onPickGallery = { uri, cfg -> onPickGalleryWallpaper(selectedWallpaperTarget, uri, cfg) }
                    )

                    SettingsSection.EDITOR -> EditorTypographySettingsPage(
                        settings = preferences.editorSettings,
                        onUpdate = onUpdateEditorSettings
                    )

                    SettingsSection.AI_CONFIG -> AiConfigSettingsPage(
                        preferences = preferences,
                        apiKeyState = apiKeyState,
                        testStatus = connectionTestStatus,
                        testMessage = connectionTestMessage,
                        onSaveKey = onSaveApiKey,
                        onDeleteKey = onDeleteApiKey,
                        onTestConnection = onTestConnection,
                        onUpdateAiConfig = onUpdateAiConfig
                    )

                    SettingsSection.BACKUP -> BackupStorageSettingsPage(
                        preferences = preferences,
                        storageSummary = storageSummary,
                        revisionCount = revisionCount,
                        onToggleAutoBackup = { enabled ->
                            onUpdateEditorSettings(preferences.editorSettings.copy(autoBackupEnabled = enabled))
                        },
                        onRunManualBackup = onRunManualBackup,
                        onRestoreAutoBackup = onRestoreAutoBackup,
                        onExportBackup = onExportFullBackup,
                        onImportBackup = onImportFullBackup,
                        onOpenRecycleBin = onOpenRecycleBin
                    )

                    SettingsSection.PRIVACY -> PrivacySecuritySettingsPage(
                        hidePreview = preferences.privacyHidePreviewInRecents,
                        onToggleHidePreview = onUpdatePrivacyHide,
                        onClearAllChats = onClearAllChats
                    )

                    SettingsSection.GUIDE_FAQ -> GuideFaqAboutPage()
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuList(
    preferences: AppPreferencesState,
    apiKeyState: ApiKeyState,
    storageSummary: StorageSummary,
    onSelectSection: (SettingsSection) -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 860.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.AutoAwesome,
                title = "Konfigurasi AI NARA & Kunci Akses",
                subtitle = if (apiKeyState.hasKey) "Kunci akses tersimpan (${apiKeyState.maskedPreview})" else "Belum ada kunci akses — ketuk untuk mengatur",
                tag = "settings_item_ai",
                onClick = { onSelectSection(SettingsSection.AI_CONFIG) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.Wallpaper,
                title = "Wallpaper Aplikasi",
                subtitle = if (preferences.useSeparateWallpapers) "Mode berbeda per halaman aktif" else "Wallpaper global dengan kontrol opasitas & blur",
                tag = "settings_item_wallpaper",
                onClick = { onSelectSection(SettingsSection.WALLPAPER) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.Person,
                title = "Profil Penulis",
                subtitle = "${preferences.profile.penName} • Target ${preferences.profile.dailyWordGoal} kata/hari",
                tag = "settings_item_profile",
                onClick = { onSelectSection(SettingsSection.PROFILE) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.Palette,
                title = "Personalisasi Tampilan & Bahasa",
                subtitle = "${preferences.themeMode.labelId} • Bahasa ${if (preferences.profile.appLanguage == "id") "Indonesia" else "English"}",
                tag = "settings_item_appearance",
                onClick = { onSelectSection(SettingsSection.APPEARANCE) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.FormatSize,
                title = "Font & Editor Novel",
                subtitle = "${EditorFontChoice.fromKey(preferences.editorSettings.fontKey).label} • ${preferences.editorSettings.fontSizeSp.toInt()} sp",
                tag = "settings_item_editor",
                onClick = { onSelectSection(SettingsSection.EDITOR) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.Backup,
                title = "Backup, Pemulihan & Penyimpanan",
                subtitle = "Terpakai ${formatBytes(storageSummary.totalBytes)} • Autosave & ekspor proyek",
                tag = "settings_item_backup",
                onClick = { onSelectSection(SettingsSection.BACKUP) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.DeleteOutline,
                title = "Tong Sampah (Recycle Bin)",
                subtitle = "Pulihkan novel, bab, atau folder yang terhapus",
                tag = "settings_item_recycle_bin",
                onClick = onOpenRecycleBin
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.Rounded.Security,
                title = "Privasi & Keamanan",
                subtitle = "Enkripsi kunci lokal dan manajemen riwayat NARA",
                tag = "settings_item_privacy",
                onClick = { onSelectSection(SettingsSection.PRIVACY) }
            )
        }
        item {
            SettingsMenuCard(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                title = "Panduan, FAQ & Tentang Aplikasi",
                subtitle = "NARA v1.0.0 • Cara pakai & tanya jawab",
                tag = "settings_item_guide",
                onClick = { onSelectSection(SettingsSection.GUIDE_FAQ) }
            )
        }
    }
}

@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    NaraPanelCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ProfileSettingsPage(
    preferences: AppPreferencesState,
    onSave: (String, String, Int, String) -> Unit
) {
    var penName by remember(preferences.profile.penName) { mutableStateOf(preferences.profile.penName) }
    var bio by remember(preferences.profile.bio) { mutableStateOf(preferences.profile.bio) }
    var goalText by remember(preferences.profile.dailyWordGoal) { mutableStateOf(preferences.profile.dailyWordGoal.toString()) }
    var lang by remember(preferences.profile.appLanguage) { mutableStateOf(preferences.profile.appLanguage) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = penName,
                        onValueChange = { penName = it },
                        label = { Text("Nama Pena / Penulis") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio Singkat / Gaya Penulisan") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Kata Harian") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Bahasa Antarmuka", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == "id",
                            onClick = { lang = "id" },
                            label = { Text("Bahasa Indonesia") }
                        )
                        FilterChip(
                            selected = lang == "en",
                            onClick = { lang = "en" },
                            label = { Text("English") }
                        )
                    }
                    Button(
                        onClick = {
                            onSave(penName, bio, goalText.toIntOrNull() ?: 1000, lang)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Profil")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsPage(
    preferences: AppPreferencesState,
    onUpdateTheme: (ThemeMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mode Warna", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onUpdateTheme(mode) },
                                label = { Text(mode.labelId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSettingsPage(
    preferences: AppPreferencesState,
    target: WallpaperPageTarget,
    onSelectTarget: (WallpaperPageTarget) -> Unit,
    onToggleSeparate: (Boolean) -> Unit,
    onUpdateConfig: (WallpaperConfig) -> Unit,
    onPickGallery: (Uri, WallpaperConfig) -> Unit
) {
    val activeConfig = preferences.wallpapers[target] ?: preferences.resolveWallpaperFor(WallpaperPageTarget.GLOBAL)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickGallery(uri, activeConfig)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wallpaper Berbeda per Halaman", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Aktifkan untuk mengatur gambar berbeda di Beranda, Perpustakaan, Editor, dan NARA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.useSeparateWallpapers,
                        onCheckedChange = { sep ->
                            onToggleSeparate(sep)
                            if (!sep) onSelectTarget(WallpaperPageTarget.GLOBAL)
                        }
                    )
                }
            }
        }

        if (preferences.useSeparateWallpapers) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WallpaperPageTarget.entries) { page ->
                        FilterChip(
                            selected = target == page,
                            onClick = { onSelectTarget(page) },
                            label = { Text(page.labelId) }
                        )
                    }
                }
            }
        }

        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sumber Wallpaper (${target.labelId})", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_wallpaper_gallery_button")
                    ) {
                        Icon(Icons.Rounded.Wallpaper, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Gambar dari Galeri Ponsel")
                    }

                    Text("Wallpaper Bawaan", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = activeConfig.sourceType == WallpaperSourceType.NONE,
                                onClick = {
                                    onUpdateConfig(activeConfig.copy(sourceType = WallpaperSourceType.NONE))
                                },
                                label = { Text("Polos Tanpa Gambar") }
                            )
                        }
                        items(BuiltinWallpaper.entries) { wp ->
                            FilterChip(
                                selected = activeConfig.sourceType == WallpaperSourceType.BUILTIN && activeConfig.builtinId == wp.id,
                                onClick = {
                                    onUpdateConfig(
                                        activeConfig.copy(
                                            sourceType = WallpaperSourceType.BUILTIN,
                                            builtinId = wp.id
                                        )
                                    )
                                },
                                label = { Text(wp.titleId) }
                            )
                        }
                    }
                }
            }
        }

        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ukuran, Posisi & Keterbacaan", style = MaterialTheme.typography.titleMedium)

                    Text("Mode Ukuran", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WallpaperScaleMode.entries) { sm ->
                            FilterChip(
                                selected = activeConfig.scaleMode == sm,
                                onClick = { onUpdateConfig(activeConfig.copy(scaleMode = sm)) },
                                label = { Text(sm.labelId) }
                            )
                        }
                    }

                    Text("Titik Jangkar", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WallpaperAlignment.entries.forEach { al ->
                            FilterChip(
                                selected = activeConfig.alignment == al,
                                onClick = { onUpdateConfig(activeConfig.copy(alignment = al)) },
                                label = { Text(al.labelId) }
                            )
                        }
                    }

                    LabeledSlider("Perbesaran / Zoom (${String.format(Locale.US, "%.2f", activeConfig.zoomScale)}x)", activeConfig.zoomScale, 1.0f..2.2f) {
                        onUpdateConfig(activeConfig.copy(zoomScale = it))
                    }
                    LabeledSlider("Posisi Horizontal (${(activeConfig.offsetX * 100).toInt()}%)", activeConfig.offsetX, -0.4f..0.4f) {
                        onUpdateConfig(activeConfig.copy(offsetX = it))
                    }
                    LabeledSlider("Posisi Vertikal (${(activeConfig.offsetY * 100).toInt()}%)", activeConfig.offsetY, -0.4f..0.4f) {
                        onUpdateConfig(activeConfig.copy(offsetY = it))
                    }
                    LabeledSlider("Opasitas Gambar (${(activeConfig.opacity * 100).toInt()}%)", activeConfig.opacity, 0.05f..1.0f) {
                        onUpdateConfig(activeConfig.copy(opacity = it))
                    }
                    LabeledSlider("Blur Latar (${activeConfig.blurRadiusDp.toInt()} dp)", activeConfig.blurRadiusDp, 0f..24f) {
                        onUpdateConfig(activeConfig.copy(blurRadiusDp = it))
                    }
                    LabeledSlider("Overlay Kontras (${(activeConfig.overlayScrimAlpha * 100).toInt()}%)", activeConfig.overlayScrimAlpha, 0f..0.85f) {
                        onUpdateConfig(activeConfig.copy(overlayScrimAlpha = it))
                    }
                    LabeledSlider("Soliditas Panel & Kartu (${(activeConfig.panelOpacity * 100).toInt()}%)", activeConfig.panelOpacity, 0.55f..0.98f) {
                        onUpdateConfig(activeConfig.copy(panelOpacity = it))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTypographySettingsPage(
    settings: EditorSettings,
    onUpdate: (EditorSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Jenis Huruf Naskah", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EditorFontChoice.entries) { choice ->
                            FilterChip(
                                selected = settings.fontKey == choice.key,
                                onClick = { onUpdate(settings.copy(fontKey = choice.key)) },
                                label = { Text(choice.label) }
                            )
                        }
                    }
                    LabeledSlider("Ukuran Huruf (${settings.fontSizeSp.toInt()} sp)", settings.fontSizeSp, 12f..28f) {
                        onUpdate(settings.copy(fontSizeSp = it))
                    }
                    LabeledSlider("Jarak Baris (${String.format(Locale.US, "%.1f", settings.lineSpacingMultiplier)}x)", settings.lineSpacingMultiplier, 1.2f..2.3f) {
                        onUpdate(settings.copy(lineSpacingMultiplier = it))
                    }
                    LabeledSlider("Margin Horizontal (${settings.horizontalMarginDp.toInt()} dp)", settings.horizontalMarginDp, 8f..48f) {
                        onUpdate(settings.copy(horizontalMarginDp = it))
                    }
                    LabeledSlider("Jarak Paragraf (${settings.paragraphSpacingDp.toInt()} dp)", settings.paragraphSpacingDp, 0f..28f) {
                        onUpdate(settings.copy(paragraphSpacingDp = it))
                    }
                    LabeledSlider("Indentasi Awal (${settings.firstLineIndentDp.toInt()} dp)", settings.firstLineIndentDp, 0f..40f) {
                        onUpdate(settings.copy(firstLineIndentDp = it))
                    }
                    LabeledSlider("Interval Autosave (${settings.autoSaveIntervalSec} detik)", settings.autoSaveIntervalSec.toFloat(), 2f..30f) {
                        onUpdate(settings.copy(autoSaveIntervalSec = it.toInt()))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiConfigSettingsPage(
    preferences: AppPreferencesState,
    apiKeyState: ApiKeyState,
    testStatus: ConnectionTestStatus,
    testMessage: String,
    onSaveKey: (String) -> Unit,
    onDeleteKey: () -> Unit,
    onTestConnection: (String?) -> Unit,
    onUpdateAiConfig: (AiEngineMode, Boolean, Boolean) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kunci Akses AI NARA", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (apiKeyState.hasKey) {
                            "Status: Tersimpan aman & terenkripsi (${apiKeyState.maskedPreview}). Masukkan kunci baru di bawah jika ingin mengubahnya."
                        } else {
                            "Status: Belum ada kunci tersimpan. Masukkan kunci akses Anda sendiri untuk mengaktifkan NARA."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(if (apiKeyState.hasKey) "Ubah Kunci Akses AI" else "Masukkan Kunci Akses AI") },
                        placeholder = { Text("Tempel kunci akses Anda di sini") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showKey = !showKey },
                                modifier = Modifier.testTag("toggle_api_key_visibility")
                            ) {
                                Icon(
                                    imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showKey) "Sembunyikan kunci" else "Tampilkan kunci"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    onSaveKey(apiKeyInput)
                                    apiKeyInput = ""
                                }
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_api_key_button")
                        ) {
                            Text(if (apiKeyState.hasKey) "Ubah Kunci" else "Simpan Kunci")
                        }

                        OutlinedButton(
                            onClick = {
                                val candidate = apiKeyInput.trim().takeIf { it.isNotEmpty() }
                                onTestConnection(candidate)
                            },
                            enabled = testStatus != ConnectionTestStatus.TESTING,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_connection_button")
                        ) {
                            if (testStatus == ConnectionTestStatus.TESTING) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Uji Koneksi")
                        }
                    }

                    if (apiKeyState.hasKey) {
                        OutlinedButton(
                            onClick = onDeleteKey,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_api_key_button")
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Kunci Tersimpan")
                        }
                    }

                    val displayStatus = if (testStatus != ConnectionTestStatus.IDLE) testStatus else preferences.aiConfig.lastConnectionStatus
                    val displayMsg = testMessage.ifBlank { preferences.aiConfig.lastConnectionMessage }
                    if (displayStatus != ConnectionTestStatus.IDLE && displayMsg.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (displayStatus) {
                                ConnectionTestStatus.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
                                ConnectionTestStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (displayStatus) {
                                        ConnectionTestStatus.SUCCESS -> Icons.Rounded.CheckCircle
                                        ConnectionTestStatus.ERROR -> Icons.Rounded.ErrorOutline
                                        else -> Icons.Rounded.Info
                                    },
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(displayMsg, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Internal NARA behavior & fallback settings (without revealing backend provider/model IDs)
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pengaturan Mesin NARA", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pilih profil pemrosesan internal untuk penyuntingan dan diskusi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AiEngineMode.entries.forEach { mode ->
                        val selected = preferences.aiConfig.engineMode == mode
                        Surface(
                            onClick = {
                                onUpdateAiConfig(
                                    mode,
                                    preferences.aiConfig.autoFallbackEnabled,
                                    preferences.aiConfig.ttsEnabled
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(mode.labelId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(mode.descriptionId, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Jalur Alternatif Otomatis", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Otomatis beralih ke mesin cadangan jika mesin utama sibuk atau gagal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.aiConfig.autoFallbackEnabled,
                            onCheckedChange = {
                                onUpdateAiConfig(preferences.aiConfig.engineMode, it, preferences.aiConfig.ttsEnabled)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fitur Suara NARA (Bacakan Bab)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Izinkan NARA membacakan bab dan hasil suntingan secara lisan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.aiConfig.ttsEnabled,
                            onCheckedChange = {
                                onUpdateAiConfig(preferences.aiConfig.engineMode, preferences.aiConfig.autoFallbackEnabled, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupStorageSettingsPage(
    preferences: AppPreferencesState,
    storageSummary: StorageSummary,
    revisionCount: Int,
    onToggleAutoBackup: (Boolean) -> Unit,
    onRunManualBackup: () -> Unit,
    onRestoreAutoBackup: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportBackup(uri) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Penyimpanan Lokal Permanen", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Total ruang terpakai: ${formatBytes(storageSummary.totalBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Database naskah: ${formatBytes(storageSummary.databaseBytes)} • Media (sampul & wallpaper): ${formatBytes(storageSummary.mediaBytes)} • Backup otomatis: ${formatBytes(storageSummary.autoBackupBytes)} • $revisionCount snapshot revisi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Backup Otomatis Lokal", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (preferences.lastAutoBackupAt > 0L) "Terakhir: ${sdf.format(Date(preferences.lastAutoBackupAt))}" else "Mencadangkan proyek secara berkala di perangkat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = preferences.editorSettings.autoBackupEnabled, onCheckedChange = onToggleAutoBackup)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRunManualBackup, modifier = Modifier.weight(1f)) {
                            Text("Backup Sekarang")
                        }
                        OutlinedButton(onClick = onRestoreAutoBackup, modifier = Modifier.weight(1f)) {
                            Text("Pulihkan Otomatis")
                        }
                    }

                    HorizontalDivider()
                    Text("Ekspor & Impor Berkas Proyek (.json)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportLauncher.launch("nara_backup_${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ekspor Backup")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Impor Backup")
                        }
                    }

                    OutlinedButton(onClick = onOpenRecycleBin, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.DeleteOutline, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Tong Sampah (Recycle Bin)")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySecuritySettingsPage(
    hidePreview: Boolean,
    onToggleHidePreview: (Boolean) -> Unit,
    onClearAllChats: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Keamanan Kunci & Naskah", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Seluruh naskah novel, catatan dunia cerita, sampul, dan wallpaper tersimpan lokal di perangkat Anda. Kunci akses AI dienkripsi menggunakan Android Keystore AES-256 dan tidak pernah ditulis ke log maupun pesan error.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sembunyikan Pratinjau Aplikasi", style = MaterialTheme.typography.titleSmall)
                            Text("Lindungi naskah saat beralih aplikasi di daftar recent apps.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = hidePreview, onCheckedChange = onToggleHidePreview)
                    }
                    OutlinedButton(
                        onClick = onClearAllChats,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hapus Seluruh Riwayat Percakapan NARA")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideFaqAboutPage() {
    val faqs = listOf(
        "Bagaimana cara mengaktifkan NARA?" to "Buka Pengaturan > Konfigurasi AI NARA, masukkan kunci akses AI Anda sendiri, tekan Simpan Kunci, lalu tekan Uji Koneksi hingga status berubah menjadi Terhubung.",
        "Apakah tulisan saya aman jika aplikasi ditutup?" to "Ya. Editor NARA memiliki autosave berkala, snapshot riwayat revisi, database lokal permanen, serta fitur backup otomatis dan ekspor JSON/Markdown/HTML.",
        "Bagaimana cara agar NARA paham karakter dan alur cerita saya?" to "Isi tab Dunia Cerita (Story Bible) di dalam detail proyek novel Anda (Karakter, Lokasi, Kronologi, Konflik, Catatan Plot). NARA otomatis membaca seluruh referensi tersebut.",
        "Bagaimana cara mengatur wallpaper agar tulisan tetap jelas?" to "Di Pengaturan > Wallpaper Aplikasi, atur Opasitas Gambar, Blur, Overlay Kontras, dan Soliditas Panel hingga teks terasa nyaman dibaca."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Panduan Singkat Penulis", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("1. Buat proyek novel & susun folder di Perpustakaan.\n2. Atur urutan bab & subbab dengan tahan ikon garis dan geser.\n3. Tulis di Editor dengan format tebal, miring, garis bawah, perataan, dan Mode Fokus.\n4. Gunakan NARA untuk kritik jujur, rewrite, cek konsistensi, atau mendengarkan naskah dibacakan.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(faqs) { (q, a) ->
            var expanded by remember { mutableStateOf(false) }
            NaraPanelCard(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = a,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        item {
            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tentang Aplikasi", style = MaterialTheme.typography.titleMedium)
                    Text("NARA — Studio Penulisan Novel Profesional", style = MaterialTheme.typography.bodyMedium)
                    Text("Versi 1.0.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
    }
}
