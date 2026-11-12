package com.example.data.repo

import android.net.Uri
import com.example.data.local.ChapterDao
import com.example.data.local.ChapterEntity
import com.example.data.local.ChapterRevisionEntity
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.NaraDatabase
import com.example.data.local.NovelDao
import com.example.data.local.NovelEntity
import com.example.data.local.RevisionDao
import com.example.data.local.StoryBibleDao
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.local.WritingStatDao
import com.example.data.local.WritingStatEntity
import com.example.data.model.NovelBackupBundle
import com.example.data.model.NovelStatus
import com.example.data.model.RichSpan
import com.example.data.model.StoryBibleCategory
import com.example.data.prefs.UserPreferencesRepository
import com.example.editor.RichTextEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NaraRepository(
    db: NaraDatabase,
    private val prefsRepo: UserPreferencesRepository,
    val backupManager: BackupAndExportManager
) {
    private val folderDao: FolderDao = db.folderDao()
    private val novelDao: NovelDao = db.novelDao()
    private val chapterDao: ChapterDao = db.chapterDao()
    private val revisionDao: RevisionDao = db.revisionDao()
    private val storyBibleDao: StoryBibleDao = db.storyBibleDao()
    private val writingStatDao: WritingStatDao = db.writingStatDao()
    private val chatDao: ChatDao = db.chatDao()

    val activeFolders: Flow<List<FolderEntity>> = folderDao.observeActiveFolders()
    val deletedFolders: Flow<List<FolderEntity>> = folderDao.observeDeletedFolders()
    val activeNovels: Flow<List<NovelEntity>> = novelDao.observeActiveNovels()
    val deletedNovels: Flow<List<NovelEntity>> = novelDao.observeDeletedNovels()
    val allActiveChapters: Flow<List<ChapterEntity>> = chapterDao.observeAllActiveChapters()
    val deletedChapters: Flow<List<ChapterEntity>> = chapterDao.observeDeletedChapters()
    val recentStats: Flow<List<WritingStatEntity>> = writingStatDao.observeRecentStats()
    val revisionCount: Flow<Int> = revisionDao.observeRevisionCount()

    fun observeNovel(novelId: Long): Flow<NovelEntity?> = novelDao.observeNovelById(novelId)
    fun observeChapters(novelId: Long): Flow<List<ChapterEntity>> = chapterDao.observeChaptersForNovel(novelId)
    fun observeChapter(chapterId: Long): Flow<ChapterEntity?> = chapterDao.observeChapterById(chapterId)
    fun observeRevisions(chapterId: Long): Flow<List<ChapterRevisionEntity>> = revisionDao.observeRevisionsForChapter(chapterId)
    fun observeStoryBible(novelId: Long): Flow<List<StoryBibleEntryEntity>> = storyBibleDao.observeEntriesForNovel(novelId)
    fun observeChatMessages(novelId: Long?): Flow<List<ChatMessageEntity>> = chatDao.observeMessagesForNovel(novelId)

    suspend fun createFolder(name: String, parentFolderId: Long? = null): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return 0L
        return folderDao.insertFolder(
            FolderEntity(name = trimmed, parentFolderId = parentFolderId)
        )
    }

    suspend fun renameFolder(folder: FolderEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            folderDao.updateFolder(folder.copy(name = trimmed))
        }
    }

    suspend fun softDeleteFolder(folderId: Long) = folderDao.softDeleteFolder(folderId)
    suspend fun restoreFolder(folderId: Long) = folderDao.restoreFolder(folderId)
    suspend fun deleteFolderPermanently(folderId: Long) = folderDao.deleteFolderPermanently(folderId)

    suspend fun createNovel(
        title: String,
        author: String,
        synopsis: String,
        genre: String,
        status: NovelStatus,
        folderId: Long? = null,
        coverPath: String = "",
        targetWordCount: Int = 50000
    ): Long {
        val now = System.currentTimeMillis()
        val novelId = novelDao.insertNovel(
            NovelEntity(
                folderId = folderId,
                title = title.trim().ifEmpty { "Novel Tanpa Judul" },
                author = author.trim(),
                synopsis = synopsis.trim(),
                genre = genre.trim().ifEmpty { "Fiksi Umum" },
                status = status.name,
                coverPath = coverPath,
                targetWordCount = targetWordCount.coerceAtLeast(1000),
                lastOpenedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
        // Create initial Chapter 1 so writer can jump straight into writing
        chapterDao.insertChapter(
            ChapterEntity(
                novelId = novelId,
                title = "Bab 1: Pembuka",
                content = "",
                sortOrder = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        triggerAutoBackupIfEnabled()
        return novelId
    }

    suspend fun updateNovel(novel: NovelEntity) {
        novelDao.updateNovel(novel.copy(updatedAt = System.currentTimeMillis()))
        triggerAutoBackupIfEnabled()
    }

    suspend fun markNovelOpened(novelId: Long) = novelDao.markNovelOpened(novelId)
    suspend fun setNovelFavorite(novelId: Long, favorite: Boolean) = novelDao.setFavorite(novelId, favorite)
    suspend fun setNovelArchived(novelId: Long, archived: Boolean) = novelDao.setArchived(novelId, archived)
    suspend fun moveNovelToFolder(novelId: Long, folderId: Long?) = novelDao.moveToFolder(novelId, folderId)
    suspend fun softDeleteNovel(novelId: Long) = novelDao.softDeleteNovel(novelId)
    suspend fun restoreNovel(novelId: Long) = novelDao.restoreNovel(novelId)
    suspend fun deleteNovelPermanently(novelId: Long) = novelDao.deleteNovelPermanently(novelId)

    suspend fun createChapter(
        novelId: Long,
        title: String,
        parentChapterId: Long? = null,
        initialContent: String = ""
    ): Long {
        val existing = chapterDao.getChaptersForNovelOnce(novelId)
        val maxOrder = existing.maxOfOrNull { it.sortOrder } ?: -1
        val now = System.currentTimeMillis()
        val wc = RichTextEngine.countWords(initialContent)
        val pc = RichTextEngine.countParagraphs(initialContent)
        val id = chapterDao.insertChapter(
            ChapterEntity(
                novelId = novelId,
                parentChapterId = parentChapterId,
                title = title.trim().ifEmpty { "Bab Baru" },
                content = initialContent,
                sortOrder = maxOrder + 1,
                wordCount = wc,
                charCount = initialContent.length,
                paragraphCount = pc,
                createdAt = now,
                updatedAt = now
            )
        )
        novelDao.markNovelOpened(novelId, now)
        triggerAutoBackupIfEnabled()
        return id
    }

    suspend fun saveChapterContent(
        chapterId: Long,
        title: String,
        content: String,
        spans: List<RichSpan>,
        alignment: String,
        notes: String,
        createRevisionLabel: String? = null
    ) {
        val existing = chapterDao.getChapterByIdOnce(chapterId) ?: return
        val normalizedSpans = RichTextEngine.normalizeSpans(spans, content.length)
        val spansJson = RichTextEngine.serializeSpans(normalizedSpans)
        val wc = RichTextEngine.countWords(content)
        val pc = RichTextEngine.countParagraphs(content)
        val now = System.currentTimeMillis()

        val wordDelta = (wc - existing.wordCount).coerceAtLeast(0)
        if (wordDelta > 0) {
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val curStat = writingStatDao.getStat(dateKey, existing.novelId)
            if (curStat != null) {
                writingStatDao.upsertStat(
                    curStat.copy(wordsWritten = curStat.wordsWritten + wordDelta, updatedAt = now)
                )
            } else {
                writingStatDao.upsertStat(
                    WritingStatEntity(dateKey = dateKey, novelId = existing.novelId, wordsWritten = wordDelta, updatedAt = now)
                )
            }
        }

        chapterDao.updateChapter(
            existing.copy(
                title = title.ifBlank { existing.title },
                content = content,
                richSpansJson = spansJson,
                alignment = alignment,
                notes = notes,
                wordCount = wc,
                charCount = content.length,
                paragraphCount = pc,
                updatedAt = now
            )
        )

        if (createRevisionLabel != null) {
            revisionDao.insertRevision(
                ChapterRevisionEntity(
                    chapterId = chapterId,
                    titleSnapshot = title,
                    contentSnapshot = content,
                    richSpansJson = spansJson,
                    wordCount = wc,
                    summaryLabel = createRevisionLabel,
                    createdAt = now
                )
            )
            revisionDao.trimOldRevisions(chapterId)
        }
        novelDao.markNovelOpened(existing.novelId, now)
    }

    suspend fun createManualRevisionSnapshot(chapterId: Long, label: String) {
        val ch = chapterDao.getChapterByIdOnce(chapterId) ?: return
        revisionDao.insertRevision(
            ChapterRevisionEntity(
                chapterId = chapterId,
                titleSnapshot = ch.title,
                contentSnapshot = ch.content,
                richSpansJson = ch.richSpansJson,
                wordCount = ch.wordCount,
                summaryLabel = label,
                createdAt = System.currentTimeMillis()
            )
        )
        revisionDao.trimOldRevisions(chapterId)
    }

    suspend fun reorderChapters(orderedIds: List<Long>) {
        chapterDao.updateChapterOrders(orderedIds)
    }

    suspend fun softDeleteChapter(chapterId: Long) = chapterDao.softDeleteChapter(chapterId)
    suspend fun restoreChapter(chapterId: Long) = chapterDao.restoreChapter(chapterId)
    suspend fun deleteChapterPermanently(chapterId: Long) = chapterDao.deleteChapterPermanently(chapterId)

    suspend fun purgeAllRecycleBin() {
        chapterDao.purgeDeletedChapters()
        novelDao.purgeDeletedNovels()
        folderDao.purgeDeletedFolders()
    }

    suspend fun upsertStoryBibleEntry(
        id: Long = 0L,
        novelId: Long,
        category: StoryBibleCategory,
        title: String,
        subtitle: String,
        content: String,
        tags: String
    ) {
        storyBibleDao.insertEntry(
            StoryBibleEntryEntity(
                id = id,
                novelId = novelId,
                category = category.name,
                title = title.trim(),
                subtitle = subtitle.trim(),
                content = content.trim(),
                tags = tags.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
        triggerAutoBackupIfEnabled()
    }

    suspend fun deleteStoryBibleEntry(id: Long) = storyBibleDao.deleteEntry(id)

    suspend fun getStoryBibleForNovelOnce(novelId: Long): List<StoryBibleEntryEntity> =
        storyBibleDao.getEntriesForNovelOnce(novelId)

    suspend fun getChaptersForNovelOnce(novelId: Long): List<ChapterEntity> =
        chapterDao.getChaptersForNovelOnce(novelId)

    suspend fun getNovelOnce(novelId: Long): NovelEntity? =
        novelDao.getNovelByIdOnce(novelId)

    suspend fun getChapterOnce(chapterId: Long): ChapterEntity? =
        chapterDao.getChapterByIdOnce(chapterId)

    suspend fun insertChatMessage(
        novelId: Long?,
        role: String,
        content: String,
        toolTag: String = "",
        contextScopeLabel: String = "",
        isError: Boolean = false
    ): Long = chatDao.insertMessage(
        ChatMessageEntity(
            novelId = novelId,
            role = role,
            content = content,
            toolTag = toolTag,
            contextScopeLabel = contextScopeLabel,
            isError = isError
        )
    )

    suspend fun updateChatMessage(id: Long, content: String, isError: Boolean = false) =
        chatDao.updateMessageContent(id, content, isError)

    suspend fun clearChat(novelId: Long?) = chatDao.clearConversation(novelId)
    suspend fun clearAllChats() = chatDao.clearAllConversations()

    suspend fun triggerAutoBackupIfEnabled() {
        val prefs = prefsRepo.state.first()
        if (!prefs.editorSettings.autoBackupEnabled) return
        val json = buildFullBackupJson()
        if (backupManager.writeAutoBackupFile(json)) {
            prefsRepo.recordAutoBackupTimestamp(System.currentTimeMillis())
        }
    }

    suspend fun buildFullBackupJson(): String {
        val folders = folderDao.getAllActiveFoldersOnce()
        val novels = novelDao.getAllActiveNovelsOnce()
        val chapters = chapterDao.getAllActiveChaptersOnce()
        val bible = storyBibleDao.getAllEntriesOnce()
        return backupManager.buildBackupJson(folders, novels, chapters, bible)
    }

    suspend fun restoreFromBackupJson(json: String): Int {
        val bundle: NovelBackupBundle = backupManager.parseBackupJson(json) ?: return 0
        val folderIdMap = mutableMapOf<Long, Long>()
        bundle.folders.sortedBy { it.parentFolderId ?: 0L }.forEach { dto ->
            val newId = folderDao.insertFolder(
                FolderEntity(
                    parentFolderId = dto.parentFolderId?.let { folderIdMap[it] },
                    name = dto.name,
                    sortOrder = dto.sortOrder,
                    createdAt = dto.createdAt
                )
            )
            folderIdMap[dto.id] = newId
        }
        val novelIdMap = mutableMapOf<Long, Long>()
        bundle.novels.forEach { dto ->
            val newId = novelDao.insertNovel(
                NovelEntity(
                    folderId = dto.folderId?.let { folderIdMap[it] },
                    title = dto.title,
                    author = dto.author,
                    synopsis = dto.synopsis,
                    genre = dto.genre,
                    status = dto.status,
                    isFavorite = dto.isFavorite,
                    isArchived = dto.isArchived,
                    targetWordCount = dto.targetWordCount,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
            )
            novelIdMap[dto.id] = newId
        }
        val chapterIdMap = mutableMapOf<Long, Long>()
        bundle.chapters.sortedBy { it.parentChapterId ?: 0L }.forEach { dto ->
            val mappedNovel = novelIdMap[dto.novelId] ?: return@forEach
            val wc = RichTextEngine.countWords(dto.content)
            val pc = RichTextEngine.countParagraphs(dto.content)
            val newId = chapterDao.insertChapter(
                ChapterEntity(
                    novelId = mappedNovel,
                    parentChapterId = dto.parentChapterId?.let { chapterIdMap[it] },
                    title = dto.title,
                    content = dto.content,
                    richSpansJson = dto.richSpansJson,
                    alignment = dto.alignment,
                    notes = dto.notes,
                    sortOrder = dto.sortOrder,
                    wordCount = wc,
                    charCount = dto.content.length,
                    paragraphCount = pc,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
            )
            chapterIdMap[dto.id] = newId
        }
        bundle.storyBibleEntries.forEach { dto ->
            val mappedNovel = novelIdMap[dto.novelId] ?: return@forEach
            storyBibleDao.insertEntry(
                StoryBibleEntryEntity(
                    novelId = mappedNovel,
                    category = dto.category,
                    title = dto.title,
                    subtitle = dto.subtitle,
                    content = dto.content,
                    tags = dto.tags,
                    sortOrder = dto.sortOrder,
                    updatedAt = dto.updatedAt
                )
            )
        }
        return bundle.novels.size
    }

    suspend fun importDocumentIntoNovel(novelId: Long, fileName: String, rawContent: String): Long {
        val cleanTitle = fileName.substringBeforeLast('.').ifBlank { "Bab Impor" }
        return createChapter(novelId = novelId, title = cleanTitle, initialContent = rawContent)
    }

    suspend fun copyImageForCover(uri: Uri): String? =
        backupManager.copyPickedImageToInternal(uri, "covers", "cover")

    suspend fun copyImageForWallpaper(uri: Uri): String? =
        backupManager.copyPickedImageToInternal(uri, "wallpapers", "wallpaper")
}
