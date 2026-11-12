package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.naraDataStore: DataStore<Preferences> by preferencesDataStore(name = "nara_user_prefs")

data class UserProfileState(
    val penName: String = "Penulis NARA",
    val bio: String = "Menulis cerita yang berbekas di hati pembaca.",
    val dailyWordGoal: Int = 1000,
    val appLanguage: String = "id"
)

data class AiConfigState(
    val engineMode: AiEngineMode = AiEngineMode.STANDARD,
    val autoFallbackEnabled: Boolean = true,
    val lastConnectionStatus: ConnectionTestStatus = ConnectionTestStatus.IDLE,
    val lastConnectionMessage: String = "",
    val ttsEnabled: Boolean = true
)

data class AppPreferencesState(
    val profile: UserProfileState = UserProfileState(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val editorSettings: EditorSettings = EditorSettings(),
    val aiConfig: AiConfigState = AiConfigState(),
    val useSeparateWallpapers: Boolean = false,
    val wallpapers: Map<WallpaperPageTarget, WallpaperConfig> = emptyMap(),
    val lastAutoBackupAt: Long = 0L,
    val privacyHidePreviewInRecents: Boolean = false
) {
    fun resolveWallpaperFor(page: WallpaperPageTarget): WallpaperConfig {
        if (!useSeparateWallpapers || page == WallpaperPageTarget.GLOBAL) {
            return wallpapers[WallpaperPageTarget.GLOBAL] ?: WallpaperConfig()
        }
        val specific = wallpapers[page]
        return if (specific != null && (specific.sourceType != WallpaperSourceType.NONE || wallpapers[page] != null)) {
            specific
        } else {
            wallpapers[WallpaperPageTarget.GLOBAL] ?: WallpaperConfig()
        }
    }
}

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.naraDataStore

    val state: Flow<AppPreferencesState> = dataStore.data.map { prefs ->
        val wallpapers = WallpaperPageTarget.entries.associateWith { target ->
            readWallpaper(prefs, target)
        }
        AppPreferencesState(
            profile = UserProfileState(
                penName = prefs[KEY_PEN_NAME] ?: "Penulis NARA",
                bio = prefs[KEY_BIO] ?: "Menulis cerita yang berbekas di hati pembaca.",
                dailyWordGoal = prefs[KEY_DAILY_GOAL] ?: 1000,
                appLanguage = prefs[KEY_LANGUAGE] ?: "id"
            ),
            themeMode = ThemeMode.fromName(prefs[KEY_THEME_MODE]),
            editorSettings = EditorSettings(
                fontKey = prefs[KEY_EDITOR_FONT] ?: "LITERATA",
                fontSizeSp = prefs[KEY_EDITOR_FONT_SIZE] ?: 17f,
                lineSpacingMultiplier = prefs[KEY_EDITOR_LINE_SPACING] ?: 1.6f,
                horizontalMarginDp = prefs[KEY_EDITOR_MARGIN] ?: 20f,
                paragraphSpacingDp = prefs[KEY_EDITOR_PARA_SPACING] ?: 12f,
                firstLineIndentDp = prefs[KEY_EDITOR_INDENT] ?: 18f,
                autoSaveIntervalSec = prefs[KEY_AUTOSAVE_SEC] ?: 4,
                autoBackupEnabled = prefs[KEY_AUTO_BACKUP_ENABLED] ?: true
            ),
            aiConfig = AiConfigState(
                engineMode = AiEngineMode.fromName(prefs[KEY_AI_ENGINE_MODE]),
                autoFallbackEnabled = prefs[KEY_AI_AUTO_FALLBACK] ?: true,
                lastConnectionStatus = runCatching {
                    ConnectionTestStatus.valueOf(prefs[KEY_AI_CONN_STATUS] ?: "IDLE")
                }.getOrDefault(ConnectionTestStatus.IDLE),
                lastConnectionMessage = prefs[KEY_AI_CONN_MSG] ?: "",
                ttsEnabled = prefs[KEY_AI_TTS_ENABLED] ?: true
            ),
            useSeparateWallpapers = prefs[KEY_USE_SEPARATE_WALLPAPERS] ?: false,
            wallpapers = wallpapers,
            lastAutoBackupAt = prefs[KEY_LAST_AUTO_BACKUP] ?: 0L,
            privacyHidePreviewInRecents = prefs[KEY_PRIVACY_HIDE] ?: false
        )
    }

    private fun readWallpaper(prefs: Preferences, target: WallpaperPageTarget): WallpaperConfig {
        val p = target.key
        val defaultSource = if (target == WallpaperPageTarget.GLOBAL) WallpaperSourceType.BUILTIN else WallpaperSourceType.BUILTIN
        return WallpaperConfig(
            sourceType = WallpaperSourceType.fromName(prefs[stringPreferencesKey("wp_${p}_source")] ?: defaultSource.name),
            builtinId = prefs[stringPreferencesKey("wp_${p}_builtin")] ?: BuiltinWallpaper.PAPER_DAWN.id,
            galleryPath = prefs[stringPreferencesKey("wp_${p}_gallery")] ?: "",
            scaleMode = WallpaperScaleMode.fromName(prefs[stringPreferencesKey("wp_${p}_scale")]),
            alignment = WallpaperAlignment.fromName(prefs[stringPreferencesKey("wp_${p}_align")]),
            zoomScale = prefs[floatPreferencesKey("wp_${p}_zoom")] ?: 1.0f,
            offsetX = prefs[floatPreferencesKey("wp_${p}_ox")] ?: 0f,
            offsetY = prefs[floatPreferencesKey("wp_${p}_oy")] ?: 0f,
            opacity = prefs[floatPreferencesKey("wp_${p}_opacity")] ?: 0.42f,
            blurRadiusDp = prefs[floatPreferencesKey("wp_${p}_blur")] ?: 0f,
            overlayScrimAlpha = prefs[floatPreferencesKey("wp_${p}_scrim")] ?: 0.28f,
            panelOpacity = prefs[floatPreferencesKey("wp_${p}_panel")] ?: 0.90f
        )
    }

    suspend fun updateProfile(penName: String, bio: String, dailyWordGoal: Int, language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PEN_NAME] = penName.ifBlank { "Penulis NARA" }
            prefs[KEY_BIO] = bio
            prefs[KEY_DAILY_GOAL] = dailyWordGoal.coerceIn(100, 20000)
            prefs[KEY_LANGUAGE] = language
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun updateEditorSettings(settings: EditorSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_EDITOR_FONT] = settings.fontKey
            prefs[KEY_EDITOR_FONT_SIZE] = settings.fontSizeSp.coerceIn(12f, 28f)
            prefs[KEY_EDITOR_LINE_SPACING] = settings.lineSpacingMultiplier.coerceIn(1.2f, 2.4f)
            prefs[KEY_EDITOR_MARGIN] = settings.horizontalMarginDp.coerceIn(8f, 48f)
            prefs[KEY_EDITOR_PARA_SPACING] = settings.paragraphSpacingDp.coerceIn(0f, 32f)
            prefs[KEY_EDITOR_INDENT] = settings.firstLineIndentDp.coerceIn(0f, 40f)
            prefs[KEY_AUTOSAVE_SEC] = settings.autoSaveIntervalSec.coerceIn(2, 60)
            prefs[KEY_AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
        }
    }

    suspend fun updateUseSeparateWallpapers(separate: Boolean) {
        dataStore.edit { it[KEY_USE_SEPARATE_WALLPAPERS] = separate }
    }

    suspend fun updateWallpaper(target: WallpaperPageTarget, config: WallpaperConfig) {
        val p = target.key
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("wp_${p}_source")] = config.sourceType.name
            prefs[stringPreferencesKey("wp_${p}_builtin")] = config.builtinId
            prefs[stringPreferencesKey("wp_${p}_gallery")] = config.galleryPath
            prefs[stringPreferencesKey("wp_${p}_scale")] = config.scaleMode.name
            prefs[stringPreferencesKey("wp_${p}_align")] = config.alignment.name
            prefs[floatPreferencesKey("wp_${p}_zoom")] = config.zoomScale.coerceIn(1.0f, 2.5f)
            prefs[floatPreferencesKey("wp_${p}_ox")] = config.offsetX.coerceIn(-0.5f, 0.5f)
            prefs[floatPreferencesKey("wp_${p}_oy")] = config.offsetY.coerceIn(-0.5f, 0.5f)
            prefs[floatPreferencesKey("wp_${p}_opacity")] = config.opacity.coerceIn(0.05f, 1.0f)
            prefs[floatPreferencesKey("wp_${p}_blur")] = config.blurRadiusDp.coerceIn(0f, 24f)
            prefs[floatPreferencesKey("wp_${p}_scrim")] = config.overlayScrimAlpha.coerceIn(0f, 0.85f)
            prefs[floatPreferencesKey("wp_${p}_panel")] = config.panelOpacity.coerceIn(0.55f, 0.98f)
        }
    }

    suspend fun updateAiConfig(engineMode: AiEngineMode, autoFallback: Boolean, ttsEnabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AI_ENGINE_MODE] = engineMode.name
            prefs[KEY_AI_AUTO_FALLBACK] = autoFallback
            prefs[KEY_AI_TTS_ENABLED] = ttsEnabled
        }
    }

    suspend fun updateConnectionStatus(status: ConnectionTestStatus, message: String) {
        dataStore.edit { prefs ->
            prefs[KEY_AI_CONN_STATUS] = status.name
            prefs[KEY_AI_CONN_MSG] = message
        }
    }

    suspend fun recordAutoBackupTimestamp(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_AUTO_BACKUP] = timestamp }
    }

    suspend fun updatePrivacyHidePreview(hide: Boolean) {
        dataStore.edit { it[KEY_PRIVACY_HIDE] = hide }
    }

    companion object {
        private val KEY_PEN_NAME = stringPreferencesKey("pen_name")
        private val KEY_BIO = stringPreferencesKey("bio")
        private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_EDITOR_FONT = stringPreferencesKey("editor_font")
        private val KEY_EDITOR_FONT_SIZE = floatPreferencesKey("editor_font_size")
        private val KEY_EDITOR_LINE_SPACING = floatPreferencesKey("editor_line_spacing")
        private val KEY_EDITOR_MARGIN = floatPreferencesKey("editor_margin")
        private val KEY_EDITOR_PARA_SPACING = floatPreferencesKey("editor_para_spacing")
        private val KEY_EDITOR_INDENT = floatPreferencesKey("editor_indent")
        private val KEY_AUTOSAVE_SEC = intPreferencesKey("autosave_sec")
        private val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val KEY_USE_SEPARATE_WALLPAPERS = booleanPreferencesKey("use_separate_wallpapers")
        private val KEY_AI_ENGINE_MODE = stringPreferencesKey("ai_engine_mode")
        private val KEY_AI_AUTO_FALLBACK = booleanPreferencesKey("ai_auto_fallback")
        private val KEY_AI_CONN_STATUS = stringPreferencesKey("ai_conn_status")
        private val KEY_AI_CONN_MSG = stringPreferencesKey("ai_conn_msg")
        private val KEY_AI_TTS_ENABLED = booleanPreferencesKey("ai_tts_enabled")
        private val KEY_LAST_AUTO_BACKUP = longPreferencesKey("last_auto_backup")
        private val KEY_PRIVACY_HIDE = booleanPreferencesKey("privacy_hide_preview")
    }
}
