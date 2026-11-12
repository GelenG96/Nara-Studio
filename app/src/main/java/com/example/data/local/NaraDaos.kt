package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    suspend fun getAllActiveFoldersOnce(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteFolder(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFolder(id: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderPermanently(id: Long)

    @Query("DELETE FROM folders WHERE isDeleted = 1")
    suspend fun purgeDeletedFolders()
}

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels WHERE isDeleted = 0 ORDER BY lastOpenedAt DESC")
    fun observeActiveNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
    fun observeNovelById(id: Long): Flow<NovelEntity?>

    @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
    suspend fun getNovelByIdOnce(id: Long): NovelEntity?

    @Query("SELECT * FROM novels WHERE isDeleted = 0")
    suspend fun getAllActiveNovelsOnce(): List<NovelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity): Long

    @Update
    suspend fun updateNovel(novel: NovelEntity)

    @Query("UPDATE novels SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun markNovelOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET folderId = :folderId, updatedAt = :now WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteNovel(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreNovel(id: Long)

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovelPermanently(id: Long)

    @Query("DELETE FROM novels WHERE isDeleted = 1")
    suspend fun purgeDeletedNovels()
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeChaptersForNovel(novelId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE isDeleted = 0")
    fun observeAllActiveChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    fun observeChapterById(chapterId: Long): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterByIdOnce(chapterId: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getChaptersForNovelOnce(novelId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE isDeleted = 0")
    suspend fun getAllActiveChaptersOnce(): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET sortOrder = :sortOrder WHERE id = :chapterId")
    suspend fun updateSortOrder(chapterId: Long, sortOrder: Int)

    @Transaction
    suspend fun updateChapterOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }

    @Query("UPDATE chapters SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteChapter(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chapters SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreChapter(id: Long)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterPermanently(id: Long)

    @Query("DELETE FROM chapters WHERE isDeleted = 1")
    suspend fun purgeDeletedChapters()
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM chapter_revisions WHERE chapterId = :chapterId ORDER BY createdAt DESC LIMIT 50")
    fun observeRevisionsForChapter(chapterId: Long): Flow<List<ChapterRevisionEntity>>

    @Query("SELECT COUNT(*) FROM chapter_revisions")
    fun observeRevisionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: ChapterRevisionEntity): Long

    @Query("DELETE FROM chapter_revisions WHERE chapterId = :chapterId AND id NOT IN (SELECT id FROM chapter_revisions WHERE chapterId = :chapterId ORDER BY createdAt DESC LIMIT 40)")
    suspend fun trimOldRevisions(chapterId: Long)
}

@Dao
interface StoryBibleDao {
    @Query("SELECT * FROM story_bible_entries WHERE novelId = :novelId ORDER BY category ASC, sortOrder ASC, title ASC")
    fun observeEntriesForNovel(novelId: Long): Flow<List<StoryBibleEntryEntity>>

    @Query("SELECT * FROM story_bible_entries WHERE novelId = :novelId ORDER BY category ASC, sortOrder ASC")
    suspend fun getEntriesForNovelOnce(novelId: Long): List<StoryBibleEntryEntity>

    @Query("SELECT * FROM story_bible_entries")
    suspend fun getAllEntriesOnce(): List<StoryBibleEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: StoryBibleEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: StoryBibleEntryEntity)

    @Query("DELETE FROM story_bible_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)
}

@Dao
interface WritingStatDao {
    @Query("SELECT * FROM writing_stats ORDER BY dateKey DESC LIMIT 30")
    fun observeRecentStats(): Flow<List<WritingStatEntity>>

    @Query("SELECT * FROM writing_stats WHERE dateKey = :dateKey AND novelId = :novelId LIMIT 1")
    suspend fun getStat(dateKey: String, novelId: Long): WritingStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: WritingStatEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE (:novelId IS NULL AND novelId IS NULL) OR novelId = :novelId ORDER BY createdAt ASC")
    fun observeMessagesForNovel(novelId: Long?): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET content = :content, isError = :isError WHERE id = :id")
    suspend fun updateMessageContent(id: Long, content: String, isError: Boolean = false)

    @Query("DELETE FROM chat_messages WHERE (:novelId IS NULL AND novelId IS NULL) OR novelId = :novelId")
    suspend fun clearConversation(novelId: Long?)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllConversations()
}
