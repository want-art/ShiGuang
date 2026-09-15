package com.shiguang.moments.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.moments.AppGraph
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MoodEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.media.ExportManager
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.reminder.CapsuleRevealWorker
import com.shiguang.moments.scoring.Level
import com.shiguang.moments.scoring.LevelCatalog
import com.shiguang.moments.util.KeyUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val repo get() = AppGraph.repo
    private val profileStore get() = AppGraph.profileStore

    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    val moments: StateFlow<List<MomentEntity>> = AppGraph.repo.allMoments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val moods: StateFlow<List<MoodEntity>> = AppGraph.repo.allMoods.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs: StateFlow<List<LogEntity>> = AppGraph.repo.recentLogs(40).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 等级：随累计瞬间自动派生 */
    val level: StateFlow<Level> = moments
        .map { LevelCatalog.levelFor(it.size) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LevelCatalog.levelFor(0))
    val levelProgress: StateFlow<Float> = moments
        .map { LevelCatalog.progressToNext(it.size) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LevelCatalog.progressToNext(0))

    private val _levelUpEvent = MutableSharedFlow<Level>(extraBufferCapacity = 4)
    val levelUpEvent: SharedFlow<Level> = _levelUpEvent.asSharedFlow()
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    // ---------- 收藏操作 ----------
    fun saveManual(sender: String, text: String, imageUris: List<Uri>, atMillis: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val before = moments.value.size
        AppGraph.repo.addManual(sender, text, imageUris, atMillis)
        val after = moments.value.size
        maybeFireSavedEvents(before, after)
    }
    fun toggleStar(id: Long) = viewModelScope.launch { AppGraph.repo.toggleStar(id) }
    fun setNote(id: Long, n: String) = viewModelScope.launch { AppGraph.repo.setNote(id, n) }
    fun setQuote(id: Long, q: String) = viewModelScope.launch { AppGraph.repo.setQuote(id, q) }
    fun deleteMoment(id: Long) = viewModelScope.launch { AppGraph.repo.deleteMoment(id) }
    fun attachImage(id: Long, uris: List<Uri>) = viewModelScope.launch {
        val before = moments.value.size
        AppGraph.repo.attachManualImage(id, uris)
        val after = moments.value.size
        maybeFireSavedEvents(before, after)
    }

    private fun maybeFireSavedEvents(before: Int, after: Int) {
        if (after <= before) return
        val idx = (before + after) % SAVE_REACTIONS.size
        _toast.tryEmit(SAVE_REACTIONS[idx])
        val oldLevel = LevelCatalog.levelFor(before)
        val newLevel = LevelCatalog.levelFor(after)
        if (newLevel.tier > oldLevel.tier) _levelUpEvent.tryEmit(newLevel)
    }

    // ---------- 偏好 ----------
    fun setOnboarded(v: Boolean) = viewModelScope.launch { profileStore.setOnboarded(v) }
    fun setThemes(t: Set<String>) = viewModelScope.launch { profileStore.setThemes(t) }
    fun setQuiet(b: Boolean, s: Int, e: Int) = viewModelScope.launch { profileStore.setQuiet(b, s, e) }
    fun setDailyReview(on: Boolean, h: Int, m: Int) = viewModelScope.launch { profileStore.setDailyReview(on, h, m) }
    fun setProfile(nickname: String, signature: String, avatarPath: String?) =
        viewModelScope.launch { profileStore.setProfile(nickname, signature, avatarPath) }
    fun setMagazineShown(weekId: String) = viewModelScope.launch { profileStore.setMagazineShown(weekId) }
    fun setContactStar(name: String, app: String, star: Boolean) = viewModelScope.launch {
        AppGraph.repo.setContactStar(name, app, star)
    }
    fun wipeAll() = viewModelScope.launch { AppGraph.repo.wipeAll() }

    // ---------- 心情打卡 ----------
    fun recordMood(emoji: String) = viewModelScope.launch {
        AppGraph.repo.recordMood(emoji)
        _toast.tryEmit("今日盖章 ✓")
    }

    // ---------- 时光宝盒 ----------
    fun scheduleCapsule(id: Long, revealAt: Long) = viewModelScope.launch {
        AppGraph.repo.scheduleCapsule(id, revealAt)
        val ctx = AppGraph.appContext ?: return@launch
        CapsuleRevealWorker.schedule(ctx, id, revealAt)
        _toast.tryEmit("宝盒已寄存 📦")
    }
    fun cancelCapsule(id: Long) = viewModelScope.launch {
        AppGraph.repo.cancelCapsule(id)
        val ctx = AppGraph.appContext ?: return@launch
        CapsuleRevealWorker.cancel(ctx, id)
    }

    // ---------- 语音秒记 ----------
    /** 同步调一次设备端语音识别 → 转写文本 → 存入文本型瞬间（实现见 VoiceCapture，下一轮接入） */
    fun listenVoiceThenSave(ctx: Context, sender: String = "我", onResult: (String?) -> Unit = {}) {
        // TODO: 接入设备端 SpeechRecognizer 后启用此调用
        onResult(null)
    }

    // ---------- 模拟注入（debug 页） ----------
    fun simulateOnce(sender: String, text: String, type: MomentType) = viewModelScope.launch {
        val before = moments.value.size
        AppGraph.repo.simulate(sender, text, type)
        val after = moments.value.size
        maybeFireSavedEvents(before, after)
    }

    // ---------- 导出 ----------
    suspend fun export(uri: Uri, asHtml: Boolean) {
        val all = AppGraph.repo.allMoments.first()
        AppGraph.repo.exportTo(uri, all, asHtml)
    }
}

// 复用 ExportManager 作为仓库的默认导出实现
private suspend fun com.shiguang.moments.data.repo.MomentRepository.exportTo(
    uri: Uri,
    all: List<MomentEntity>,
    asHtml: Boolean,
) {
    val ctx = AppGraph.appContext ?: return
    if (asHtml) ExportManager.exportHtml(ctx, uri, all) else ExportManager.exportJson(ctx, uri, all)
}

// ====== 辅助：本周末第几周 ======
fun currentWeekIdLocal(): String = KeyUtil.currentWeekId()

/** 保存成功后展示的暖卡片文案池（每次轮换一条） */
val SAVE_REACTIONS = listOf(
    "给今天的自己：这件事值得被记住 🌤",
    "生活里的小发光体，又亮了一个 ✨",
    "好好的，你已经把今天过成了诗 ✍️",
    "这一笔，会让多年后的你笑出来 😊",
    "给今天的自己：慢慢来，你做得很好 🧡",
    "又多了一段可以回望的日子 🪐",
)