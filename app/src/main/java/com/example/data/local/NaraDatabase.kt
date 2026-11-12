package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FolderEntity::class,
        NovelEntity::class,
        ChapterEntity::class,
        ChapterRevisionEntity::class,
        StoryBibleEntryEntity::class,
        WritingStatEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NaraDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun revisionDao(): RevisionDao
    abstract fun storyBibleDao(): StoryBibleDao
    abstract fun writingStatDao(): WritingStatDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: NaraDatabase? = null

        fun getInstance(context: Context): NaraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NaraDatabase::class.java,
                    "nara_writer_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
