package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.ai.NaraTtsPlayer
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.local.WritingStatEntity
import com.example.data.model.AiEngineMode
import com.example.data.model.ConnectionTestStatus
import com.example.data.model.EditorSettings
import com.example.data.model.NovelStatus
import com.example.data.model.StoryBibleCategory
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.ApiKeyState
import com.example.data.prefs.AppPreferencesState
import com.example.data.repo.StorageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainAppViewModel(private val container: AppContainer) : ViewModel() {
    val repo = container.repository
    val prefsRepo = container.preferencesRepository
    val secureKeyStore = container.secureKeyStore
    val aiClient = container.aiClient

    val ttsPlayer = NaraTtsPlayer(aiClient, viewModelScope)

    val preferences: StateFlow<AppPreferencesState> = prefsRepo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferencesState())

    val apiKeyState: StateFlow<ApiKeyState> = secureKeyStore.keyState

    val activeFolders: StateFlow<List<FolderEntity>> = repo.activeFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedFolders: StateFlow<List<FolderEntity>> = repo.deletedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNovels: StateFlow<List<NovelEntity>> = repo.activeNovels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedNovels: StateFlow<List<NovelEntity>> = repo.deletedNovels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveChapters: StateFlow<List<ChapterEntity>> = repo.allActiveChapters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedChapters: StateFlow<List<ChapterEntity>> = repo.deletedChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStats: StateFlow<List<WritingStatEntity>> = repo.recentStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisionCount: StateFlow<Int> = repo.revisionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _activeContextNovelId = MutableStateFlow<Long?>(null)
    val activeContextNovelId: StateFlow<Long?> = _activeContextNovelId.asStateFlow()

    private val _activeContextChapterId = MutableStateFlow<Long?>(null)
    val activeContextChapterId: StateFlow<Long?> = _activeContextChapterId.asStateFlow()

    private val _selectedSnippetForAi = MutableStateFlow("")
    val selectedSnippetForAi: StateFlow<String> = _selectedSnippetForAi.asStateFlow()

    private val _storageSummary = MutableStateFlow(StorageSummary(0L, 0L, 0L, 0L))
    val storageSummary: StateFlow<StorageSummary> = _storageSummary.asStateFlow()

    private val _connectionTestStatus = MutableStateFlow(ConnectionTestStatus.IDLE)
    val connectionTestStatus: StateFlow<ConnectionTestStatus> = _connectionTestStatus.asStateFlow()

    private val _connectionTestMessage = MutableStateFlow("")
    val connectionTestMessage: StateFlow<String> = _connectionTestMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshStorageSummary()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun setActiveContext(novelId: Long?, chapterId: Long? = null, selectedSnippet: String = "") {
        _activeContextNovelId.value = novelId
        if (chapterId != null) _activeContextChapterId.value = chapterId
        if (selectedSnippet.isNotBlank()) _selectedSnippetForAi.value = selectedSnippet
        if (novelId != null) {
            viewModelScope.launch { repo.markNovelOpened(novelId) }
        }
    }

    fun refreshStorageSummary() {
        viewModelScope.launch {
            _storageSummary.value = repo.backupManager.computeStorageSummary()
        }
    }

    // Folder actions
    fun createFolder(name: String, parentId: Long? = null) {
        viewModelScope.launch {
            repo.createFolder(name, parentId)
            showToast("Folder \"$name\" dibuat")
        }
    }

    fun renameFolder(folder: FolderEntity, newName: String) {
        viewModelScope.launch {
            repo.renameFolder(folder, newName)
            showToast("Nama folder diperbarui")
        }
    }

    fun softDeleteFolder(folderId: Long) {
        viewModelScope.launch {
            repo.softDeleteFolder(folderId)
            showToast("Folder dipindahkan ke Tong Sampah")
        }
    }

    // Novel actions
    fun createNovel(
        title: String,
        author: String,
        synopsis: String,
        genre: String,
        status: NovelStatus,
        folderId: Long?,
        coverUri: Uri?,
        targetWordCount: Int,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val coverPath = if (coverUri != null) {
                repo.copyImageForCover(coverUri).orEmpty()
            } else ""
            val id = repo.createNovel(
                title = title,
                author = author,
                synopsis = synopsis,
                genre = genre,
                status = status,
                folderId = folderId,
                coverPath = coverPath,
                targetWordCount = targetWordCount
            )
            _activeContextNovelId.value = id
            refreshStorageSummary()
            onCreated(id)
        }
    }

    fun updateNovelWithOptionalCover(novel: NovelEntity, newCoverUri: Uri?) {
        viewModelScope.launch {
            val coverPath = if (newCoverUri != null) {
                repo.copyImageForCover(newCoverUri) ?: novel.coverPath
            } else novel.coverPath
            repo.updateNovel(novel.copy(coverPath = coverPath))
            refreshStorageSummary()
            showToast("Info novel disimpan")
        }
    }

    fun toggleFavorite(novel: NovelEntity) {
        viewModelScope.launch {
            repo.setNovelFavorite(novel.id, !novel.isFavorite)
        }
    }

    fun toggleArchive(novel: NovelEntity) {
        viewModelScope.launch {
            val target = !novel.isArchived
            repo.setNovelArchived(novel.id, target)
            showToast(if (target) "Novel diarsipkan" else "Novel dikeluarkan dari arsip")
        }
    }

    fun moveNovelToFolder(novelId: Long, folderId: Long?) {
        viewModelScope.launch {
            repo.moveNovelToFolder(novelId, folderId)
            showToast("Novel dipindahkan")
        }
    }

    fun softDeleteNovel(novelId: Long) {
        viewModelScope.launch {
            repo.softDeleteNovel(novelId)
            showToast("Novel dipindahkan ke Tong Sampah")
        }
    }

    // Chapter actions
    fun createChapter(
        novelId: Long,
        title: String,
        parentChapterId: Long? = null,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repo.createChapter(novelId, title, parentChapterId)
            onCreated(id)
        }
    }

    fun reorderChapters(orderedIds: List<Long>) {
        viewModelScope.launch {
            repo.reorderChapters(orderedIds)
        }
    }

    fun softDeleteChapter(chapterId: Long) {
        viewModelScope.launch {
            repo.softDeleteChapter(chapterId)
            showToast("Bab dipindahkan ke Tong Sampah")
        }
    }

    // Story Bible
    fun saveStoryBibleEntry(
        id: Long = 0L,
        novelId: Long,
        category: StoryBibleCategory,
        title: String,
        subtitle: String,
        content: String,
        tags: String
    ) {
        viewModelScope.launch {
            repo.upsertStoryBibleEntry(id, novelId, category, title, subtitle, content, tags)
            showToast("Referensi cerita disimpan")
        }
    }

    fun deleteStoryBibleEntry(id: Long) {
        viewModelScope.launch {
            repo.deleteStoryBibleEntry(id)
            showToast("Catatan referensi dihapus")
        }
    }

    // Recycle Bin actions
    fun restoreNovel(id: Long) = viewModelScope.launch { repo.restoreNovel(id); showToast("Novel dipulihkan") }
    fun deleteNovelPermanently(id: Long) = viewModelScope.launch { repo.deleteNovelPermanently(id); refreshStorageSummary() }
    fun restoreChapter(id: Long) = viewModelScope.launch { repo.restoreChapter(id); showToast("Bab dipulihkan") }
    fun deleteChapterPermanently(id: Long) = viewModelScope.launch { repo.deleteChapterPermanently(id); refreshStorageSummary() }
    fun restoreFolder(id: Long) = viewModelScope.launch { repo.restoreFolder(id); showToast("Folder dipulihkan") }
    fun deleteFolderPermanently(id: Long) = viewModelScope.launch { repo.deleteFolderPermanently(id) }
    fun purgeAllRecycleBin() = viewModelScope.launch {
        repo.purgeAllRecycleBin()
        refreshStorageSummary()
        showToast("Tong Sampah dikosongkan")
    }

    // Preferences & Wallpaper
    fun updateProfile(penName: String, bio: String, dailyWordGoal: Int, language: String) {
        viewModelScope.launch {
            prefsRepo.updateProfile(penName, bio, dailyWordGoal, language)
            showToast("Profil penulis disimpan")
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.updateThemeMode(mode) }
    }

    fun updateEditorSettings(settings: EditorSettings) {
        viewModelScope.launch { prefsRepo.updateEditorSettings(settings) }
    }

    fun updateUseSeparateWallpapers(separate: Boolean) {
        viewModelScope.launch { prefsRepo.updateUseSeparateWallpapers(separate) }
    }

    fun updateWallpaper(target: WallpaperPageTarget, config: WallpaperConfig) {
        viewModelScope.launch {
            prefsRepo.updateWallpaper(target, config)
        }
    }

    fun setWallpaperFromGallery(target: WallpaperPageTarget, uri: Uri, baseConfig: WallpaperConfig) {
        viewModelScope.launch {
            val savedPath = repo.copyImageForWallpaper(uri)
            if (savedPath != null) {
                prefsRepo.updateWallpaper(
                    target,
                    baseConfig.copy(
                        sourceType = com.example.data.model.WallpaperSourceType.GALLERY,
                        galleryPath = savedPath
                    )
                )
                refreshStorageSummary()
                showToast("Wallpaper galeri diterapkan")
            } else {
                showToast("Gagal membaca gambar dari galeri")
            }
        }
    }

    fun updatePrivacyHidePreview(hide: Boolean) {
        viewModelScope.launch { prefsRepo.updatePrivacyHidePreview(hide) }
    }

    // AI Config & Secure Key Management
    fun saveApiKey(rawKey: String) {
        if (secureKeyStore.saveApiKey(rawKey)) {
            _connectionTestStatus.value = ConnectionTestStatus.IDLE
            _connectionTestMessage.value = "Kunci tersimpan aman secara terenkripsi. Tekan Uji Koneksi untuk memverifikasi."
            showToast("Kunci akses AI tersimpan aman")
        } else {
            showToast("Gagal menyimpan kunci. Pastikan kolom tidak kosong.")
        }
    }

    fun deleteApiKey() {
        secureKeyStore.deleteApiKey()
        _connectionTestStatus.value = ConnectionTestStatus.IDLE
        _connectionTestMessage.value = ""
        viewModelScope.launch {
            prefsRepo.updateConnectionStatus(ConnectionTestStatus.IDLE, "")
        }
        showToast("Kunci akses AI dihapus")
    }

    fun testAiConnection(unsavedKeyInput: String? = null) {
        viewModelScope.launch {
            _connectionTestStatus.value = ConnectionTestStatus.TESTING
            _connectionTestMessage.value = "Menguji koneksi ke layanan NARA..."
            val aiCfg = preferences.value.aiConfig
            val result = aiClient.testConnection(
                candidateKey = unsavedKeyInput,
                engineMode = aiCfg.engineMode,
                autoFallback = aiCfg.autoFallbackEnabled
            )
            val status = if (result.success) ConnectionTestStatus.SUCCESS else ConnectionTestStatus.ERROR
            _connectionTestStatus.value = status
            _connectionTestMessage.value = result.message
            prefsRepo.updateConnectionStatus(status, result.message)
        }
    }

    fun updateAiEngineConfig(mode: AiEngineMode, autoFallback: Boolean, ttsEnabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAiConfig(mode, autoFallback, ttsEnabled)
            showToast("Konfigurasi mesin NARA diperbarui")
        }
    }

    fun clearAllNaraChats() {
        viewModelScope.launch {
            repo.clearAllChats()
            showToast("Seluruh riwayat percakapan NARA dihapus")
        }
    }

    // Backup & Export / Import
    fun runManualAutoBackupNow() {
        viewModelScope.launch {
            val json = repo.buildFullBackupJson()
            val ok = repo.backupManager.writeAutoBackupFile(json)
            if (ok) {
                prefsRepo.recordAutoBackupTimestamp(System.currentTimeMillis())
                refreshStorageSummary()
                showToast("Backup lokal berhasil dibuat")
            } else {
                showToast("Gagal membuat backup lokal")
            }
        }
    }

    fun restoreFromAutoBackup() {
        viewModelScope.launch {
            val json = repo.backupManager.readAutoBackupFile()
            if (json.isNullOrBlank()) {
                showToast("Belum ada file backup otomatis")
                return@launch
            }
            val count = repo.restoreFromBackupJson(json)
            refreshStorageSummary()
            showToast("Berhasil memulihkan $count proyek novel dari backup otomatis")
        }
    }

    fun exportFullBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val json = repo.buildFullBackupJson()
            val ok = repo.backupManager.writeTextToUri(uri, json)
            showToast(if (ok) "Backup proyek berhasil diekspor" else "Gagal mengekspor backup")
        }
    }

    fun importFullBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = repo.backupManager.readTextFromUri(uri)
            if (text.isNullOrBlank()) {
                showToast("File backup kosong atau tidak terbaca")
                return@launch
            }
            val count = repo.restoreFromBackupJson(text)
            refreshStorageSummary()
            if (count > 0) {
                showToast("Berhasil mengimpor $count proyek novel")
            } else {
                showToast("Format file backup tidak dikenali")
            }
        }
    }

    fun exportNovelMarkdownToUri(novelId: Long, uri: Uri) {
        viewModelScope.launch {
            val novel = repo.getNovelOnce(novelId) ?: return@launch
            val chapters = repo.getChaptersForNovelOnce(novelId)
            val content = repo.backupManager.exportNovelAsMarkdown(novel, chapters)
            val ok = repo.backupManager.writeTextToUri(uri, content)
            showToast(if (ok) "Naskah novel berhasil diekspor" else "Gagal mengekspor novel")
        }
    }

    fun importDocumentAsChapter(novelId: Long, fileName: String, uri: Uri) {
        viewModelScope.launch {
            val text = repo.backupManager.readTextFromUri(uri)
            if (text == null) {
                showToast("Gagal membaca dokumen")
                return@launch
            }
            repo.importDocumentIntoNovel(novelId, fileName, text)
            refreshStorageSummary()
            showToast("Dokumen diimpor sebagai bab baru")
        }
    }

    fun observeNovel(novelId: Long) = repo.observeNovel(novelId)
    fun observeChapters(novelId: Long) = repo.observeChapters(novelId)
    fun observeStoryBible(novelId: Long) = repo.observeStoryBible(novelId)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainAppViewModel(container) as T
                }
            }
    }
}
