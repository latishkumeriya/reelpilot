package com.reelpilot.app.manager

import com.reelpilot.app.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 brain: decides WHEN the timer is allowed to count.
 * - Timer runs only if user pressed Start
 * - AND a selected target app (Instagram / YouTube / TikTok) is in foreground
 *   (if "pause until target app" is ON) OR always (if OFF)
 * - AND screen is ON
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val prefs: PrefsRepository,
    private val timer: ScrollTimerManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _instagramForeground = MutableStateFlow(false)
    val instagramForeground: StateFlow<Boolean> = _instagramForeground

    private val _targetForeground = MutableStateFlow(false)
    val targetForeground: StateFlow<Boolean> = _targetForeground

    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage

    private val _screenOn = MutableStateFlow(true)
    val screenOn: StateFlow<Boolean> = _screenOn

    private val _pauseReason = MutableStateFlow<String?>(null)
    val pauseReason: StateFlow<String?> = _pauseReason

    private var userSessionActive = false

    fun setUserSessionActive(active: Boolean) {
        userSessionActive = active
        scope.launch { reevaluate() }
    }

    /** Legacy single-app API — kept for compat. */
    fun setInstagramForeground(foreground: Boolean) {
        if (_instagramForeground.value == foreground) return
        _instagramForeground.value = foreground
        scope.launch { reevaluate() }
    }

    /** Phase 4: package-aware foreground tracking. */
    fun setForegroundPackage(pkg: String?) {
        _foregroundPackage.value = pkg
        scope.launch {
            val enabled = enabledPackages()
            val isTarget = pkg != null && enabled.contains(pkg)
            _targetForeground.value = isTarget
            _instagramForeground.value = (pkg == AppDetector.INSTAGRAM_PKG)
            reevaluate()
        }
    }

    fun setScreenOn(on: Boolean) {
        _screenOn.value = on
        scope.launch { reevaluate() }
    }

    private suspend fun enabledPackages(): Set<String> {
        val set = mutableSetOf<String>()
        if (prefs.enableInstagram.first()) set.add(AppDetector.INSTAGRAM_PKG)
        if (prefs.enableYoutube.first()) set.add(AppDetector.YOUTUBE_PKG)
        if (prefs.enableTiktok.first()) {
            set.add(AppDetector.TIKTOK_PKG)
            set.add(AppDetector.TIKTOK_LITE_PKG)
        }
        return set
    }

    private suspend fun reevaluate() {
        if (!userSessionActive) return
        val autoStart = prefs.autoStart.first()
        val wantRunning = (!autoStart || _targetForeground.value) && _screenOn.value

        if (wantRunning) {
            _pauseReason.value = null
            if (timer.state.value == ScrollState.PAUSED && timer.wasPausedBySystem) {
                timer.resumeInternal()
            }
        } else {
            val reason = when {
                !_screenOn.value -> "Screen off — paused"
                autoStart && !_targetForeground.value -> {
                    val cur = _foregroundPackage.value
                    if (cur == null) "Waiting for target app…"
                    else "Waiting — in ${AppDetector.label(cur)}"
                }
                else -> "Paused"
            }
            _pauseReason.value = reason
            if (timer.state.value == ScrollState.RUNNING) {
                timer.pauseBySystem()
            }
        }
    }
}
