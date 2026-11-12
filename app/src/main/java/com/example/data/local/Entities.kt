package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "novels",
    indices = [Index("folderId"), Index("isDeleted"), Index("isArchived")]
)
data class NovelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val author: String = "",
    val synopsis: String = "",
    val genre: String = "Fiksi Umum",
    val status: String = "WRITING",
    val coverPath: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val targetWordCount: Int = 50000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId"), Index("parentChapterId"), Index("isDeleted")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val parentChapterId: Long? = null,
    val title: String,
    val content: String = "",
    val richSpansJson: String = "[]",
    val alignment: String = "START",
    val notes: String = "",
    val sortOrder: Int = 0,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val paragraphCount: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapter_revisions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class ChapterRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val titleSnapshot: String,
    val contentSnapshot: String,
    val richSpansJson: String,
    val wordCount: Int,
    val summaryLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "story_bible_entries",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId"), Index("category")]
)
data class StoryBibleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val category: String,
    val title: String,
    val subtitle: String = "",
    val content: String = "",
    val tags: String = "",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "writing_stats",
    indices = [Index(value = ["dateKey", "novelId"], unique = true)]
)
data class WritingStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val novelId: Long,
    val wordsWritten: Int = 0,
    val sessionSeconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("novelId"), Index("createdAt")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long? = null,
    val role: String, // "user" or "model"
    val content: String,
    val toolTag: String = "",
    val contextScopeLabel: String = "",
    val isError: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
