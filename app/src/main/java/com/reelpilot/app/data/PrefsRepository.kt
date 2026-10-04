package com.reelpilot.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "reelpilot_prefs")

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val INTERVAL_SEC = intPreferencesKey("interval_sec")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val RESET_ON_MANUAL = booleanPreferencesKey("reset_on_manual")
        val VIBRATE_ON_SCROLL = booleanPreferencesKey("vibrate_on_scroll")
        val MAX_REELS = intPreferencesKey("max_reels") // 0 = unlimited
        val ENABLE_INSTAGRAM = booleanPreferencesKey("enable_instagram")
        val ENABLE_YOUTUBE = booleanPreferencesKey("enable_youtube")
        val ENABLE_TIKTOK = booleanPreferencesKey("enable_tiktok")
        val TOTAL_REELS = intPreferencesKey("total_reels")
        val TOTAL_SESSIONS = intPreferencesKey("total_sessions")
        val SOUND_ON_SCROLL = booleanPreferencesKey("sound_on_scroll")
        val RESTART_ON_BOOT = booleanPreferencesKey("restart_on_boot")
        val SESSION_WAS_ACTIVE = booleanPreferencesKey("session_was_active")
        val OVERLAY_SCALE = floatPreferencesKey("overlay_scale")
        val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        const val DEFAULT_INTERVAL = 35
    }

    val intervalSec: Flow<Int> = context.dataStore.data.map { it[INTERVAL_SEC] ?: DEFAULT_INTERVAL }
    val autoStart: Flow<Boolean> = context.dataStore.data.map { it[AUTO_START] ?: true }
    val resetOnManual: Flow<Boolean> = context.dataStore.data.map { it[RESET_ON_MANUAL] ?: true }
    val vibrateOnScroll: Flow<Boolean> = context.dataStore.data.map { it[VIBRATE_ON_SCROLL] ?: false }
    val maxReels: Flow<Int> = context.dataStore.data.map { it[MAX_REELS] ?: 0 }

    val enableInstagram: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_INSTAGRAM] ?: true }
    val enableYoutube: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_YOUTUBE] ?: false }
    val enableTiktok: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_TIKTOK] ?: false }

    val totalReels: Flow<Int> = context.dataStore.data.map { it[TOTAL_REELS] ?: 0 }
    val totalSessions: Flow<Int> = context.dataStore.data.map { it[TOTAL_SESSIONS] ?: 0 }

    val soundOnScroll: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ON_SCROLL] ?: false }
    val restartOnBoot: Flow<Boolean> = context.dataStore.data.map { it[RESTART_ON_BOOT] ?: false }
    val sessionWasActive: Flow<Boolean> = context.dataStore.data.map { it[SESSION_WAS_ACTIVE] ?: false }
    val overlayScale: Flow<Float> = context.dataStore.data.map { it[OVERLAY_SCALE] ?: 1f }
    val overlayAlpha: Flow<Float> = context.dataStore.data.map { it[OVERLAY_ALPHA] ?: 0.94f }

    suspend fun setInterval(sec: Int) {
        context.dataStore.edit { it[INTERVAL_SEC] = sec.coerceIn(5, 300) }
    }
    suspend fun setAutoStart(v: Boolean) { context.dataStore.edit { it[AUTO_START] = v } }
    suspend fun setResetOnManual(v: Boolean) { context.dataStore.edit { it[RESET_ON_MANUAL] = v } }
    suspend fun setVibrate(v: Boolean) { context.dataStore.edit { it[VIBRATE_ON_SCROLL] = v } }
    suspend fun setMaxReels(v: Int) { context.dataStore.edit { it[MAX_REELS] = v.coerceIn(0, 200) } }

    suspend fun setEnableInstagram(v: Boolean) { context.dataStore.edit { it[ENABLE_INSTAGRAM] = v } }
    suspend fun setEnableYoutube(v: Boolean) { context.dataStore.edit { it[ENABLE_YOUTUBE] = v } }
    suspend fun setEnableTiktok(v: Boolean) { context.dataStore.edit { it[ENABLE_TIKTOK] = v } }

    suspend fun recordSession() {
        context.dataStore.edit { it[TOTAL_SESSIONS] = (it[TOTAL_SESSIONS] ?: 0) + 1 }
    }

    suspend fun recordScroll() {
        context.dataStore.edit { it[TOTAL_REELS] = (it[TOTAL_REELS] ?: 0) + 1 }
    }

    suspend fun resetStats() {
        context.dataStore.edit {
            it[TOTAL_REELS] = 0
            it[TOTAL_SESSIONS] = 0
        }
    }

    suspend fun setSound(v: Boolean) { context.dataStore.edit { it[SOUND_ON_SCROLL] = v } }
    suspend fun setRestartOnBoot(v: Boolean) { context.dataStore.edit { it[RESTART_ON_BOOT] = v } }
    suspend fun setSessionWasActive(v: Boolean) { context.dataStore.edit { it[SESSION_WAS_ACTIVE] = v } }
    suspend fun setOverlayScale(v: Float) { context.dataStore.edit { it[OVERLAY_SCALE] = v.coerceIn(0.8f, 1.3f) } }
    suspend fun setOverlayAlpha(v: Float) { context.dataStore.edit { it[OVERLAY_ALPHA] = v.coerceIn(0.4f, 1f) } }
}
