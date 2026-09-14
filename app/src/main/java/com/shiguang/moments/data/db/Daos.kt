package com.shiguang.moments.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shiguang.moments.data.models.ContactEntity
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.PromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Insert suspend fun insert(m: MomentEntity): Long
    @Update suspend fun update(m: MomentEntity)
    @Delete suspend fun delete(m: MomentEntity)
    @Query("DELETE FROM moments") suspend fun deleteAll()

    @Query("SELECT * FROM moments ORDER BY capturedAt DESC")
    fun all(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments ORDER BY capturedAt DESC LIMIT 200")
    suspend fun recent(): List<MomentEntity>

    @Query("SELECT * FROM moments WHERE id = :id")
    suspend fun byId(id: Long): MomentEntity?

    @Query("SELECT * FROM moments WHERE imagePath IS NOT NULL ORDER BY capturedAt DESC")
    fun images(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE starred = 1 ORDER BY capturedAt DESC")
    fun starred(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE sender = :sender ORDER BY capturedAt DESC")
    suspend fun bySender(sender: String): List<MomentEntity>

    @Query("SELECT COUNT(*) FROM moments") suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM moments WHERE capturedAt >= :dayStart AND capturedAt < :dayEnd")
    suspend fun countInRange(dayStart: Long, dayEnd: Long): Int

    /** 相册归集：找最近一条图片型、尚未有图、且时间接近的瞬间 */
    @Query("SELECT * FROM moments WHERE type = 'IMAGE' AND imagePath IS NULL ORDER BY capturedAt DESC LIMIT 20")
    suspend fun recentImageMomentsWithoutMedia(): List<MomentEntity>
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: ContactEntity)
    @Query("SELECT * FROM contacts ORDER BY moments DESC, lastSeen DESC")
    fun all(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name = :name AND app = :app LIMIT 1")
    suspend fun byKey(name: String, app: String): ContactEntity?

    @Query("UPDATE contacts SET isStar = :star WHERE id = :id")
    suspend fun setStar(id: Long, star: Boolean)

    @Query("SELECT * FROM contacts WHERE isStar = 1 ORDER BY lastSeen DESC")
    fun stars(): Flow<List<ContactEntity>>
}

@Dao
interface LogDao {
    @Insert suspend fun insert(l: LogEntity)
    @Query("SELECT * FROM logs ORDER BY ts DESC LIMIT :n")
    fun recent(n: Int): Flow<List<LogEntity>>
    @Query("SELECT COUNT(*) FROM logs") suspend fun count(): Int
}

@Dao
interface PromptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: PromptEntity)
    @Query("SELECT * FROM prompts WHERE pid = :pid LIMIT 1") suspend fun byId(pid: String): PromptEntity?
    @Query("UPDATE prompts SET saved = 1 WHERE pid = :pid") suspend fun markSaved(pid: String)
}