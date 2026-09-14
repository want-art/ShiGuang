package com.shiguang.moments.data.repo

import android.content.Context
import android.net.Uri
import com.shiguang.moments.data.db.AppDatabase
import com.shiguang.moments.data.models.ChatMessage
import com.shiguang.moments.data.models.ContactEntity
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.data.models.PromptEntity
import com.shiguang.moments.data.models.Verdict
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.prefs.ProfileStore
import com.shiguang.moments.scoring.ScoringEngine
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.service.Notifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID

/**
 * 记忆仓库：一切「入库」的唯一入口（监听、无障碍、手动、相册归集都汇到这里）。
 */
class MomentRepository(private val appContext: Context) {

    private val db = AppDatabase.get(appContext)
    private val momentDao = db.momentDao()
    private val contactDao = db.contactDao()
    private val logDao = db.logDao()
    private val promptDao = db.promptDao()
    private val profileStore = ProfileStore(appContext)

    val allMoments: Flow<List<MomentEntity>> = momentDao.all()
    val images: Flow<List<MomentEntity>> = momentDao.images()

    fun recentLogs(n: Int): Flow<List<LogEntity>> = logDao.recent(n)
    suspend fun countMoments(): Long = momentDao.count().toLong()

    /** 核心管线：一条聊天消息 → 日志 → 判分 → 收藏或提示 */
    suspend fun ingest(msg: ChatMessage, isStar: Boolean, burstCount: Int): Verdict {
        val profile = profileStore.profile.first()
        val result = ScoringEngine.evaluate(msg, profile, isStar, burstCount)
        val quiet = isQuiet(profile, System.currentTimeMillis())

        // 该消息可能在档案打开前到达（如刚装好还没完成引导）；此时不打扰
        logDao.insert(
            LogEntity(
                ts = System.currentTimeMillis(), app = msg.app, sender = msg.sender,
                text = msg.text, type = msg.type, score = result.score,
                verdict = result.tier, reason = result.reasons.joinToString("；"),
                processedAs = "none",
            )
        )
        upsertContact(msg.app, msg.sender)

        return when (result.tier) {
            Verdict.IGNORED -> Verdict.IGNORED
            Verdict.PROMPT -> {
                if (quiet) { markLogLast(msg, result, "quiet-skip"); return Verdict.PROMPT }
                val pid = createPrompt(msg)
                Notifier.postPrompt(appContext, pid, msg)
                markLogLast(msg, result, "prompt")
                Verdict.PROMPT
            }
            Verdict.AUTO -> {
                if (!profile.autoSaveEnabled) {
                    val pid = createPrompt(msg)
                    if (!quiet) Notifier.postPrompt(appContext, pid, msg)
                    markLogLast(msg, result, "prompt")
                    return Verdict.PROMPT
                }
                val id = createMoment(msg, result.score, result.reasons, source = "auto")
                markLogLast(msg, result, "auto")
                if (!quiet) Notifier.postSaved(appContext, id, msg.sender)
                Verdict.AUTO
            }
        }
    }

    private suspend fun markLogLast(msg: ChatMessage, r: com.shiguang.moments.data.models.ScoreResult, processed: String) {
        // 日志里 processedAs 用最后一条即可：这里追加一条带 processedAs 的日志避免更新语句
        logDao.insert(
            LogEntity(
                ts = System.currentTimeMillis(), app = msg.app, sender = msg.sender,
                text = msg.text, type = msg.type, score = r.score,
                verdict = r.tier, reason = r.reasons.joinToString("；"), processedAs = processed,
            )
        )
    }

    /** 生成一条待办提示（点通知即收藏） */
    suspend fun createPrompt(msg: ChatMessage): String {
        val pid = UUID.randomUUID().toString()
        val payload = JSONObject()
            .put("app", msg.app).put("sender", msg.sender)
            .put("text", msg.text).put("type", msg.type.name)
            .put("sentAt", msg.sentAt)
        promptDao.insert(PromptEntity(pid, payload.toString(), System.currentTimeMillis()))
        return pid
    }

    /** 用户点了「收藏」：按 pid 落库 */
    suspend fun finishPrompt(pid: String): Long? {
        val p = promptDao.byId(pid) ?: return null
        if (p.saved) return null
        val o = JSONObject(p.payloadJson)
        val msg = ChatMessage(
            app = o.getString("app"), sender = o.getString("sender"),
            text = o.getString("text"), type = MomentType.valueOf(o.getString("type")),
            sentAt = o.getLong("sentAt"),
        )
        val id = createMoment(msg, 0, listOf("手动收藏"), source = "tap")
        promptDao.markSaved(pid)
        Notifier.postSaved(appContext, id, msg.sender)
        return id
    }

    /** 直接建一条瞬间 */
    suspend fun createMoment(
        msg: ChatMessage,
        score: Int,
        reasons: List<String>,
        source: String,
        imagePath: String? = null,
        typeOverride: MomentType? = null,
    ): Long {
        val id = momentDao.insert(
            MomentEntity(
                capturedAt = msg.sentAt,
                createdAt = System.currentTimeMillis(),
                app = msg.app, sender = msg.sender,
                type = typeOverride ?: msg.type,
                text = msg.text,
                imagePath = imagePath,
                themeTags = tagsFor(msg.text),
                score = score,
                source = source,
            )
        )
        bumpContactMoment(msg.app, msg.sender)
        return id
    }

    /** 手动收藏（详情页/首页“随手记”）：type 强制 TEXT */
    suspend fun addManual(sender: String, text: String, imageUri: Uri?): Long {
        var path: String? = null
        if (imageUri != null) path = ImageStore.copyToLocal(appContext, imageUri)
        val msg = ChatMessage(
            app = "manual", sender = sender.ifBlank { "我" },
            text = text, type = if (path != null) MomentType.IMAGE else MomentType.TEXT,
            sentAt = System.currentTimeMillis(),
        )
        return createMoment(msg, 0, emptyList(), source = "manual", imagePath = path)
    }

    /** 相册归集：把一张新图挂到最近未配图的图片瞬间上 */
    suspend fun attachGalleryImage(displayName: String, mediaId: String, uri: Uri, nearTs: Long): Boolean {
        if (!profileStore.profile.first().galleryImportEnabled) return false
        val candidates = momentDao.recentImageMomentsWithoutMedia().take(20)
        val window = 10 * 60 * 1000L
        val target = candidates.firstOrNull { kotlin.math.abs(it.capturedAt - nearTs) <= window }
            ?: return false
        val path = ImageStore.copyToLocal(appContext, uri)
        if (path == null) return false
        val updated = target.copy(imagePath = path, mediaSourceId = mediaId, source = "gallery")
        momentDao.update(updated)
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
    suspend fun attachManualImage(id: Long, uri: Uri): Boolean {
        val m = momentDao.byId(id) ?: return false
        val path = ImageStore.copyToLocal(appContext, uri) ?: return false
        momentDao.update(m.copy(imagePath = path, mediaSourceId = "manual"))
        return true
    }
    suspend fun wipeAll() {
        momentDao.deleteAll()
    }

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
        // 同步到 DataStore 的 starContacts，方便监听端快速查询
        val profile = profileStore.profile.first()
        val key = "${app}${""}$name"
        val set = profile.starContacts.toMutableSet()
        if (star) set += key else set -= key
        profileStore.setStarContacts(set)
    }

    fun isQuiet(profile: Profile, now: Long): Boolean {
        if (!profile.quietSleeping) return false
        val h = java.util.Calendar.getInstance().apply { timeInMillis = now }
            .get(java.util.Calendar.HOUR_OF_DAY)
        val start = profile.quietStartHour
        val end = profile.quietEndHour
        return if (start <= end) h in start until end else (h >= start || h < end)
    }
}