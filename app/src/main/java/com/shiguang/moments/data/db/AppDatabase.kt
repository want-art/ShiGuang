package com.shiguang.moments.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.shiguang.moments.data.models.ContactEntity
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MoodEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.PromptEntity

@Database(
    entities = [MomentEntity::class, ContactEntity::class, LogEntity::class, PromptEntity::class, MoodEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
    abstract fun contactDao(): ContactDao
    abstract fun logDao(): LogDao
    abstract fun promptDao(): PromptDao
    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE moments ADD COLUMN imagePaths TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE moments SET imagePaths = imagePath WHERE imagePath IS NOT NULL AND imagePath != '' AND (imagePaths = '' OR imagePaths IS NULL)")
        }
        private val MIGRATION_2_3 = Migration(2, 3) { db ->
            // 时光宝盒：两条新列
            db.execSQL("ALTER TABLE moments ADD COLUMN capsuleRevealAt INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE moments ADD COLUMN capsuleRevealedAt INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_moments_capsuleRevealAt ON moments(capsuleRevealAt)")
            // 心情表（day 唯一索引）
            db.execSQL("CREATE TABLE IF NOT EXISTS moods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, emoji TEXT NOT NULL, ts INTEGER NOT NULL, day INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_moods_day ON moods(day)")
        }
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shiguang.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}