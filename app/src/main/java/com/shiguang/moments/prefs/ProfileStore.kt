package com.shiguang.moments.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shiguang_profile")

/** 用户偏好档案快照（手动模式 + 个人页） */
data class Profile(
    val onboarded: Boolean = false,
    val themes: Set<String> = emptySet(),
    val starContacts: Set<String> = emptySet(),
    val quietSleeping: Boolean = false,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 7,
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 30,
    val nickname: String = "拾光的朋友",
    val signature: String = "",
    val avatarPath: String? = null,
    val lastMagazineWeekId: String? = null,
) {
    fun starSetFor(app: String): Set<String> =
        starContacts.filter { it.substringBefore(SEP) == app }.mapTo(mutableSetOf()) { it.substringAfter(SEP) }

    companion object {
        const val SEP = ""
    }
}

class ProfileStore(private val appCtx: Context) {
    private val dataStore = appCtx.dataStore

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_THEMES = stringSetPreferencesKey("themes")
        private val KEY_STARS = stringSetPreferencesKey("stars")
        private val KEY_QUIET = booleanPreferencesKey("quiet")
        private val KEY_QUIET_START = intPreferencesKey("quiet_start")
        private val KEY_QUIET_END = intPreferencesKey("quiet_end")
        private val KEY_REVIEW_EN = booleanPreferencesKey("review_en")
        private val KEY_REVIEW_H = intPreferencesKey("review_h")
        private val KEY_REVIEW_M = intPreferencesKey("review_m")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_SIGNATURE = stringPreferencesKey("signature")
        private val KEY_AVATAR = stringPreferencesKey("avatar")
        private val KEY_MAG_WEEK = stringPreferencesKey("magazine_week")
    }

    val profile: Flow<Profile> = dataStore.data.map { p ->
        Profile(
            onboarded = p[KEY_ONBOARDED] ?: false,
            themes = p[KEY_THEMES] ?: emptySet(),
            starContacts = p[KEY_STARS] ?: emptySet(),
            quietSleeping = p[KEY_QUIET] ?: false,
            quietStartHour = p[KEY_QUIET_START] ?: 23,
            quietEndHour = p[KEY_QUIET_END] ?: 7,
            dailyReviewEnabled = p[KEY_REVIEW_EN] ?: true,
            dailyReviewHour = p[KEY_REVIEW_H] ?: 21,
            dailyReviewMinute = p[KEY_REVIEW_M] ?: 30,
            nickname = p[KEY_NICKNAME] ?: "拾光的朋友",
            signature = p[KEY_SIGNATURE] ?: "",
            avatarPath = p[KEY_AVATAR],
            lastMagazineWeekId = p[KEY_MAG_WEEK],
        )
    }

    suspend fun setOnboarded(v: Boolean) = dataStore.edit { it[KEY_ONBOARDED] = v }
    suspend fun setThemes(t: Set<String>) = dataStore.edit { it[KEY_THEMES] = t }
    suspend fun setStarContacts(s: Set<String>) = dataStore.edit { it[KEY_STARS] = s }
    suspend fun setQuiet(b: Boolean, start: Int, end: Int) = dataStore.edit {
        it[KEY_QUIET] = b; it[KEY_QUIET_START] = start; it[KEY_QUIET_END] = end
    }
    suspend fun setDailyReview(on: Boolean, hour: Int, minute: Int) = dataStore.edit {
        it[KEY_REVIEW_EN] = on; it[KEY_REVIEW_H] = hour; it[KEY_REVIEW_M] = minute
    }
    suspend fun setProfile(nickname: String, signature: String, avatarPath: String?) = dataStore.edit {
        it[KEY_NICKNAME] = nickname.ifBlank { "拾光的朋友" }
        it[KEY_SIGNATURE] = signature
        if (avatarPath.isNullOrBlank()) it.remove(KEY_AVATAR) else it[KEY_AVATAR] = avatarPath
    }
    suspend fun setMagazineShown(weekId: String) = dataStore.edit { it[KEY_MAG_WEEK] = weekId }
}