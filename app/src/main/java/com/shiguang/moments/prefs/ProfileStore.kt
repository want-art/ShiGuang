package com.shiguang.moments.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shiguang_profile")

/** 用户偏好档案快照 */
data class Profile(
    val onboarded: Boolean = false,
    val themes: Set<String> = emptySet(),          // 选中主题 slug，如 love/friendship
    val trackedApps: Set<String> = setOf(WEIXIN, QQ), // 监听的应用包名
    val starContacts: Set<String> = emptySet(),    // "app\x1Fname"
    val remindThreshold: Int = 50,
    val autoThreshold: Int = 78,
    val autoSaveEnabled: Boolean = false,          // 授权后自动记录开关
    val listeningEnabled: Boolean = true,
    val a11yEnabled: Boolean = false,
    val galleryImportEnabled: Boolean = true,
    val quietSleeping: Boolean = false,            // 夜间不打扰
    val quietStartHour: Int = 23,                  // 62 表示未启用；23:30 用 JH 存分钟更直观——这里简化存小时
    val quietEndHour: Int = 7,
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 30,
) {
    fun starSetFor(app: String): Set<String> = starContacts.filter { it.substringBefore('') == app }.mapTo(mutableSetOf()) { it.substringAfter('') }
    companion object { const val WEIXIN = "com.tencent.mm"; const val QQ = "com.tencent.mobileqq" }
}

class ProfileStore(private val appCtx: Context) {
    private val dataStore = appCtx.dataStore

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_THEMES = stringSetPreferencesKey("themes")
        private val KEY_APPS = stringSetPreferencesKey("apps")
        private val KEY_STARS = stringSetPreferencesKey("stars")
        private val KEY_REMIND_T = intPreferencesKey("remind_t")
        private val KEY_AUTO_T = intPreferencesKey("auto_t")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val KEY_LISTEN = booleanPreferencesKey("listen")
        private val KEY_A11Y = booleanPreferencesKey("a11y")
        private val KEY_GALLERY = booleanPreferencesKey("gallery")
        private val KEY_QUIET = booleanPreferencesKey("quiet")
        private val KEY_QUIET_START = intPreferencesKey("quiet_start")
        private val KEY_QUIET_END = intPreferencesKey("quiet_end")
        private val KEY_REVIEW_EN = booleanPreferencesKey("review_en")
        private val KEY_REVIEW_H = intPreferencesKey("review_h")
        private val KEY_REVIEW_M = intPreferencesKey("review_m")
    }

    val profile: Flow<Profile> = dataStore.data.map { p ->
        Profile(
            onboarded = p[KEY_ONBOARDED] ?: false,
            themes = p[KEY_THEMES] ?: emptySet(),
            trackedApps = p[KEY_APPS] ?: setOf(Profile.WEIXIN, Profile.QQ),
            starContacts = p[KEY_STARS] ?: emptySet(),
            remindThreshold = p[KEY_REMIND_T] ?: 50,
            autoThreshold = p[KEY_AUTO_T] ?: 78,
            autoSaveEnabled = p[KEY_AUTO_SAVE] ?: false,
            listeningEnabled = p[KEY_LISTEN] ?: true,
            a11yEnabled = p[KEY_A11Y] ?: false,
            galleryImportEnabled = p[KEY_GALLERY] ?: true,
            quietSleeping = p[KEY_QUIET] ?: false,
            quietStartHour = p[KEY_QUIET_START] ?: 23,
            quietEndHour = p[KEY_QUIET_END] ?: 7,
            dailyReviewEnabled = p[KEY_REVIEW_EN] ?: true,
            dailyReviewHour = p[KEY_REVIEW_H] ?: 21,
            dailyReviewMinute = p[KEY_REVIEW_M] ?: 30,
        )
    }

    suspend fun setOnboarded(v: Boolean) = dataStore.edit { it[KEY_ONBOARDED] = v }
    suspend fun setThemes(t: Set<String>) = dataStore.edit { it[KEY_THEMES] = t }
    suspend fun setApps(a: Set<String>) = dataStore.edit { it[KEY_APPS] = a }
    suspend fun setStarContacts(s: Set<String>) = dataStore.edit { it[KEY_STARS] = s }
    suspend fun addStar(app: String, name: String) {
        val key = "${app}${name}"
        dataStore.edit { set -> val cur = (set[KEY_STARS] ?: emptySet()).toMutableSet(); cur += key; set[KEY_STARS] = cur }
    }
    suspend fun removeStar(app: String, name: String) {
        val key = "${app}${name}"
        dataStore.edit { set -> val cur = (set[KEY_STARS] ?: emptySet()).toMutableSet(); cur -= key; set[KEY_STARS] = cur }
    }
    suspend fun setRemindThreshold(v: Int) = dataStore.edit { it[KEY_REMIND_T] = v.coerceIn(20, 100) }
    suspend fun setAutoThreshold(v: Int) = dataStore.edit { it[KEY_AUTO_T] = v.coerceIn(30, 100) }
    suspend fun setAutoSave(b: Boolean) = dataStore.edit { it[KEY_AUTO_SAVE] = b }
    suspend fun setListening(b: Boolean) = dataStore.edit { it[KEY_LISTEN] = b }
    suspend fun setA11y(b: Boolean) = dataStore.edit { it[KEY_A11Y] = b }
    suspend fun setGalleryImport(b: Boolean) = dataStore.edit { it[KEY_GALLERY] = b }
    suspend fun setQuiet(b: Boolean, start: Int, end: Int) = dataStore.edit {
        it[KEY_QUIET] = b; it[KEY_QUIET_START] = start; it[KEY_QUIET_END] = end
    }
    suspend fun setDailyReview(on: Boolean, hour: Int, minute: Int) = dataStore.edit {
        it[KEY_REVIEW_EN] = on; it[KEY_REVIEW_H] = hour; it[KEY_REVIEW_M] = minute
    }
}