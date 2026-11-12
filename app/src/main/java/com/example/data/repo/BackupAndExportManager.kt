package com.example.data.repo

import android.content.Context
import android.net.Uri
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.ChapterBackupDto
import com.example.data.model.FolderBackupDto
import com.example.data.model.NovelBackupBundle
import com.example.data.model.NovelBackupDto
import com.example.data.model.StoryBibleBackupDto
import com.example.editor.RichTextEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class StorageSummary(
    val totalBytes: Long,
    val databaseBytes: Long,
    val mediaBytes: Long,
    val autoBackupBytes: Long
)

class BackupAndExportManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val bundleAdapter = moshi.adapter(NovelBackupBundle::class.java).indent("  ")

    suspend fun copyPickedImageToInternal(uri: Uri, folderName: String, filePrefix: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(appContext.filesDir, folderName).apply { if (!exists()) mkdirs() }
                val targetFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                targetFile.absolutePath
            }.getOrNull()
        }

    fun buildBackupJson(
        folders: List<FolderEntity>,
        novels: List<NovelEntity>,
        chapters: List<ChapterEntity>,
        bibleEntries: List<StoryBibleEntryEntity>
    ): String {
        val bundle = NovelBackupBundle(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            folders = folders.map {
                FolderBackupDto(it.id, it.parentFolderId, it.name, it.sortOrder, it.createdAt)
            },
            novels = novels.map {
                NovelBackupDto(
                    id = it.id,
                    folderId = it.folderId,
                    title = it.title,
                    author = it.author,
                    synopsis = it.synopsis,
                    genre = it.genre,
                    status = it.status,
                    isFavorite = it.isFavorite,
                    isArchived = it.isArchived,
                    targetWordCount = it.targetWordCount,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            chapters = chapters.map {
                ChapterBackupDto(
                    id = it.id,
                    novelId = it.novelId,
                    parentChapterId = it.parentChapterId,
                    title = it.title,
                    content = it.content,
                    richSpansJson = it.richSpansJson,
                    alignment = it.alignment,
                    notes = it.notes,
                    sortOrder = it.sortOrder,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            storyBibleEntries = bibleEntries.map {
                StoryBibleBackupDto(
                    id = it.id,
                    novelId = it.novelId,
                    category = it.category,
                    title = it.title,
                    subtitle = it.subtitle,
                    content = it.content,
                    tags = it.tags,
                    sortOrder = it.sortOrder,
                    updatedAt = it.updatedAt
                )
            }
        )
        return bundleAdapter.toJson(bundle)
    }

    fun parseBackupJson(json: String): NovelBackupBundle? =
        runCatching { bundleAdapter.fromJson(json) }.getOrNull()

    suspend fun writeAutoBackupFile(json: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(appContext.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val file = File(dir, AUTO_BACKUP_FILENAME)
            file.writeText(json, Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    suspend fun readAutoBackupFile(): String? = withContext(Dispatchers.IO) {
        val file = File(File(appContext.filesDir, "backups"), AUTO_BACKUP_FILENAME)
        if (!file.exists()) return@withContext null
        runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    suspend fun writeTextToUri(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext false
            true
        }.getOrDefault(false)
    }

    suspend fun readTextFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
        }.getOrNull()
    }

    fun exportNovelAsMarkdown(novel: NovelEntity, chapters: List<ChapterEntity>): String {
        val sb = StringBuilder()
        sb.append(novel.title).append("\n")
        if (novel.author.isNotBlank()) sb.append("Oleh: ").append(novel.author).append("\n")
        sb.append("Genre: ").append(novel.genre).append("\n")
        if (novel.synopsis.isNotBlank()) {
            sb.append("\nSinopsis:\n").append(novel.synopsis).append("\n")
        }
        sb.append("\n========================================\n\n")
        chapters.forEach { ch ->
            val prefix = if (ch.parentChapterId != null) "  [Subbab] " else ""
            sb.append(prefix).append(ch.title).append("\n\n")
            sb.append(ch.content.trimEnd()).append("\n\n----------------------------------------\n\n")
        }
        return sb.toString()
    }

    fun exportChapterAsHtml(chapter: ChapterEntity): String {
        val spans = RichTextEngine.deserializeSpans(chapter.richSpansJson)
        return RichTextEngine.toHtml(chapter.title, chapter.content, spans)
    }

    suspend fun computeStorageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        val dbFile = appContext.getDatabasePath("nara_writer_db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val coversDir = File(appContext.filesDir, "covers")
        val wallpapersDir = File(appContext.filesDir, "wallpapers")
        val mediaBytes = folderSize(coversDir) + folderSize(wallpapersDir)
        val backupsDir = File(appContext.filesDir, "backups")
        val backupBytes = folderSize(backupsDir)
        StorageSummary(
            totalBytes = dbBytes + mediaBytes + backupBytes,
            databaseBytes = dbBytes,
            mediaBytes = mediaBytes,
            autoBackupBytes = backupBytes
        )
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        private const val AUTO_BACKUP_FILENAME = "auto_backup_latest.json"
    }
}
