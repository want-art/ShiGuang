package com.shiguang.moments.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.moments.AppGraph
import com.shiguang.moments.data.models.LogEntity
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.media.ExportManager
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.service.ListeningService
import com.shiguang.moments.data.models.MomentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val repo get() = AppGraph.repo
    private val profileStore get() = AppGraph.profileStore

    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    val moments: StateFlow<List<MomentEntity>> = AppGraph.repo.allMoments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs: StateFlow<List<LogEntity>> = AppGraph.repo.recentLogs(40).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- 收藏操作 ----------
    fun saveManual(sender: String, text: String, imageUri: Uri?) = viewModelScope.launch {
        AppGraph.repo.addManual(sender, text, imageUri)
    }
    fun toggleStar(id: Long) = viewModelScope.launch { AppGraph.repo.toggleStar(id) }
    fun setNote(id: Long, n: String) = viewModelScope.launch { AppGraph.repo.setNote(id, n) }
    fun setQuote(id: Long, q: String) = viewModelScope.launch { AppGraph.repo.setQuote(id, q) }
    fun deleteMoment(id: Long) = viewModelScope.launch { AppGraph.repo.deleteMoment(id) }
    fun attachImage(id: Long, uri: Uri) = viewModelScope.launch { AppGraph.repo.attachManualImage(id, uri) }

    // ---------- 偏好 ----------
    fun setThemes(t: Set<String>) = viewModelScope.launch { profileStore.setThemes(t) }
    fun setOnboarded(v: Boolean) = viewModelScope.launch { profileStore.setOnboarded(v) }
    fun setApps(t: Set<String>) = viewModelScope.launch { profileStore.setApps(t) }
    fun setRemindThreshold(v: Int) = viewModelScope.launch { profileStore.setRemindThreshold(v) }
    fun setAutoThreshold(v: Int) = viewModelScope.launch { profileStore.setAutoThreshold(v) }
    fun setAutoSave(b: Boolean) = viewModelScope.launch { profileStore.setAutoSave(b) }
    fun setListening(b: Boolean) = viewModelScope.launch { profileStore.setListening(b) }
    fun setA11y(b: Boolean) = viewModelScope.launch { profileStore.setA11y(b) }
    fun setGallery(b: Boolean) = viewModelScope.launch { profileStore.setGalleryImport(b) }
    fun setQuiet(b: Boolean, s: Int, e: Int) = viewModelScope.launch { profileStore.setQuiet(b, s, e) }
    fun setDailyReview(on: Boolean, h: Int, m: Int) = viewModelScope.launch { profileStore.setDailyReview(on, h, m) }
    fun setContactStar(name: String, app: String, star: Boolean) = viewModelScope.launch {
        AppGraph.repo.setContactStar(name, app, star)
    }
    fun wipeAll() = viewModelScope.launch { AppGraph.repo.wipeAll() }

    // ---------- 模拟注入（debug 页） ----------
    fun simulateOnce(sender: String, text: String, type: MomentType) {
        ListeningService.inject(sender, "com.tencent.mm", text, type)
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