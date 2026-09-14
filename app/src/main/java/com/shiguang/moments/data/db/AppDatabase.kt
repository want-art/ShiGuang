package com.shiguang.moments.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shiguang.moments.data.models.ContactEntity
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.PromptEntity

@Database(
    entities = [MomentEntity::class, ContactEntity::class, LogEntity::class, PromptEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
    abstract fun contactDao(): ContactDao
    abstract fun logDao(): LogDao
    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shiguang.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}