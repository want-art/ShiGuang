package com.shiguang.moments.data.repo

import android.content.Context
import android.net.Uri
import com.shiguang.moments.data.db.AppDatabase
import com.shiguang.moments.data.models.ChatMessage
import com.shiguang.moments.data.models.ContactEntity
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MoodEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.prefs.ProfileStore
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.service.Notifier
import com.shiguang.moments.util.KeyUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 记忆仓库：手动记录、心情打卡、时光宝盒。
 */
class MomentRepository(private val appContext: Context) {

    private val db = AppDatabase.get(appContext)
    private val momentDao = db.momentDao()
    private val contactDao = db.contactDao()
    private val logDao = db.logDao()
    private val moodDao = db.moodDao()
    private val profileStore = ProfileStore(appContext)

    val allMoments: Flow<List<MomentEntity>> = momentDao.all()
    val images: Flow<List<MomentEntity>> = momentDao.images()
    val allMoods: Flow<List<MoodEntity>> = moodDao.all()

    fun recentLogs(n: Int): Flow<List<LogEntity>> = logDao.recent(n)
    suspend fun countMoments(): Long = momentDao.count().toLong()

    /** 直接建一条瞬间（被 Notifier.postSaved 内部调用） */
    suspend fun createMoment(
        msg: ChatMessage,
        score: Int,
        reasons: List<String>,
        source: String,
        imagePaths: List<String> = emptyList(),
        typeOverride: MomentType? = null,
    ): Long {
        val primary = imagePaths.firstOrNull()
        val id = momentDao.insert(
            MomentEntity(
                capturedAt = msg.sentAt,
                createdAt = System.currentTimeMillis(),
                app = msg.app, sender = msg.sender,
                type = typeOverride ?: msg.type,
                text = msg.text,
                imagePath = primary,
                imagePaths = imagePaths.joinToString(","),
                themeTags = tagsFor(msg.text),
                score = score,
                source = source,
            )
        )
        bumpContactMoment(msg.app, msg.sender)
        return id
    }

    /** 手动收藏：支持多张图片 → 一条瞬间含多图 */
    suspend fun addManual(sender: String, text: String, imageUris: List<Uri>): Long {
        val paths = imageUris.mapNotNull { ImageStore.copyToLocal(appContext, it) }
        val type = if (paths.isNotEmpty()) MomentType.IMAGE else MomentType.TEXT
        val msg = ChatMessage(
            app = "manual", sender = sender.ifBlank { "我" },
            text = text, type = type,
            sentAt = System.currentTimeMillis(),
        )
        val id = createMoment(msg, 0, emptyList(), source = "manual", imagePaths = paths)
        Notifier.postSaved(appContext, id, msg.sender)
        return id
    }

    /** 语音秒记：把语音转写后的文字记成一条文本型瞬间 */
    suspend fun addVoiceText(text: String, sender: String = "我"): Long {
        val msg = ChatMessage(app = "manual", sender = sender, text = text, type = MomentType.TEXT,
            sentAt = System.currentTimeMillis())
        val id = createMoment(msg, 0, emptyList(), source = "voice")
        Notifier.postSaved(appContext, id, sender)
        return id
    }

    /** 为已有瞬间追加图片（多张） */
    suspend fun attachManualImage(id: Long, uris: List<Uri>): Boolean {
        val m = momentDao.byId(id) ?: return false
        val paths = uris.mapNotNull { ImageStore.copyToLocal(appContext, it) }
        if (paths.isEmpty()) return false
        val merged = (m.allImagePaths() + paths).distinct()
        momentDao.update(
            m.copy(
                imagePath = merged.first(),
                imagePaths = merged.joinToString(","),
                mediaSourceId = if (m.mediaSourceId != null) m.mediaSourceId else "manual",
            )
        )
        return true
    }

    suspend fun toggleStar(id: Long) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(starred = !m.starred))
    }
    suspend fun setNote(id: Long, note: String) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(note = note.ifBlank { null }))
    }
    suspend fun setQuote(id: Long, quote: String) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(quote = quote.ifBlank { null }))
    }
    suspend fun deleteMoment(id: Long) {
        momentDao.byId(id)?.let { momentDao.delete(it) }
    }
    suspend fun wipeAll() {
        momentDao.deleteAll()
    }

    /** 调试模拟注入：直接构造瞬间，绕开任何自动逻辑 */
    suspend fun simulate(sender: String, text: String, type: MomentType) {
        val msg = ChatMessage(app = "manual", sender = sender, text = text, type = type,
            sentAt = System.currentTimeMillis())
        val id = createMoment(msg, 0, emptyList(), source = "sim")
        Notifier.postSaved(appContext, id, msg.sender)
    }

    // ---------- 心情打卡 ----------
    suspend fun recordMood(emoji: String) {
        val day = KeyUtil.currentDayLong()
        moodDao.upsert(MoodEntity(emoji = emoji, ts = System.currentTimeMillis(), day = day))
    }
    suspend fun moodForDay(day: Long): MoodEntity? = moodDao.byDay(day)
    suspend fun moodInRange(start: Long, end: Long): List<MoodEntity> = moodDao.range(start, end)

    // ---------- 时光宝盒 ----------
    /** 把已保存的瞬间预约为宝盒；调用方需另行安排 WorkManager（由 VM 触发） */
    suspend fun scheduleCapsule(id: Long, revealAt: Long) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(capsuleRevealAt = revealAt, capsuleRevealedAt = null))
    }
    suspend fun cancelCapsule(id: Long) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(capsuleRevealAt = null))
    }
    suspend fun markCapsuleRevealed(id: Long) {
        val m = momentDao.byId(id) ?: return
        momentDao.update(m.copy(capsuleRevealedAt = System.currentTimeMillis()))
    }
    suspend fun dueCapsules(nowTs: Long = System.currentTimeMillis()): List<MomentEntity> =
        momentDao.dueCapsules(nowTs)

    private suspend fun tagsFor(text: String): String {
        val profile = profileStore.profile.first()
        val set = LinkedHashSet<String>()
        for (t in ThemeCatalog.ALL) {
            if (t.slug in profile.themes && t.keywords.any { text.contains(it) }) set += t.slug
        }
        if (ThemeCatalog.GENERAL.any { text.contains(it) }) set += "general"
        return set.joinToString(",")
    }

    private suspend fun upsertContact(app: String, name: String) {
        val now = System.currentTimeMillis()
        val existing = contactDao.byKey(name, app)
        contactDao.upsert(
            ContactEntity(
                id = existing?.id ?: 0, name = name, app = app,
                isStar = existing?.isStar ?: false,
                lastSeen = now, moments = existing?.moments ?: 0,
            )
        )
    }
    private suspend fun bumpContactMoment(app: String, name: String) {
        val existing = contactDao.byKey(name, app) ?: upsertContact(app, name).let { contactDao.byKey(name, app) }
        if (existing != null) contactDao.upsert(existing.copy(moments = existing.moments + 1, lastSeen = System.currentTimeMillis()))
    }

    suspend fun setContactStar(name: String, app: String, star: Boolean) {
        val c = contactDao.byKey(name, app)
        if (c != null) { contactDao.setStar(c.id, star) }
        else {
            contactDao.upsert(ContactEntity(name = name, app = app, isStar = star, lastSeen = System.currentTimeMillis()))
        }
        val profile = profileStore.profile.first()
        val key = "${app}${"$"}$name"
        val set = profile.starContacts.toMutableSet()
        if (star) set += key else set -= key
        profileStore.setStarContacts(set)
    }
}