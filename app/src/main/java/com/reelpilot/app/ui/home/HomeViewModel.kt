package com.reelpilot.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.manager.ScrollTimerManager
import com.reelpilot.app.manager.SessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: PrefsRepository,
    val timer: ScrollTimerManager,
    val coordinator: SessionCoordinator
) : ViewModel() {

    val interval: StateFlow<Int> = prefs.intervalSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrefsRepository.DEFAULT_INTERVAL)
    val autoStart: StateFlow<Boolean> = prefs.autoStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val resetOnManual: StateFlow<Boolean> = prefs.resetOnManual
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val vibrate: StateFlow<Boolean> = prefs.vibrateOnScroll
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val maxReels: StateFlow<Int> = prefs.maxReels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val enableInstagram: StateFlow<Boolean> = prefs.enableInstagram
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val enableYoutube: StateFlow<Boolean> = prefs.enableYoutube
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val enableTiktok: StateFlow<Boolean> = prefs.enableTiktok
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val totalReels: StateFlow<Int> = prefs.totalReels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalSessions: StateFlow<Int> = prefs.totalSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val sound: StateFlow<Boolean> = prefs.soundOnScroll
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val restartOnBoot: StateFlow<Boolean> = prefs.restartOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val overlayScale: StateFlow<Float> = prefs.overlayScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)
    val overlayAlpha: StateFlow<Float> = prefs.overlayAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.94f)

    val state: StateFlow<ScrollState> = timer.state
    val remaining: StateFlow<Int> = timer.remaining
    val reels: StateFlow<Int> = timer.reelsScrolled
    val pauseReason: StateFlow<String?> = coordinator.pauseReason
    val instagramForeground: StateFlow<Boolean> = coordinator.instagramForeground
    val foregroundPackage: StateFlow<String?> = coordinator.foregroundPackage

    fun setInterval(sec: Int) = viewModelScope.launch { prefs.setInterval(sec) }
    fun setAutoStart(v: Boolean) = viewModelScope.launch { prefs.setAutoStart(v) }
    fun setReset(v: Boolean) = viewModelScope.launch { prefs.setResetOnManual(v) }
    fun setVibrate(v: Boolean) = viewModelScope.launch { prefs.setVibrate(v) }
    fun setMaxReels(v: Int) = viewModelScope.launch { prefs.setMaxReels(v) }

    fun setEnableInstagram(v: Boolean) = viewModelScope.launch { prefs.setEnableInstagram(v) }
    fun setEnableYoutube(v: Boolean) = viewModelScope.launch { prefs.setEnableYoutube(v) }
    fun setEnableTiktok(v: Boolean) = viewModelScope.launch { prefs.setEnableTiktok(v) }

    fun resetStats() = viewModelScope.launch { prefs.resetStats() }
    fun snooze() = viewModelScope.launch { timer.addSeconds(10) }
    fun setSound(v: Boolean) = viewModelScope.launch { prefs.setSound(v) }
    fun setRestartOnBoot(v: Boolean) = viewModelScope.launch { prefs.setRestartOnBoot(v) }
    fun setOverlayScale(v: Float) = viewModelScope.launch { prefs.setOverlayScale(v) }
    fun setOverlayAlpha(v: Float) = viewModelScope.launch { prefs.setOverlayAlpha(v) }

    fun start() = viewModelScope.launch { timer.start() }
    fun stop() = viewModelScope.launch { timer.stop() }
    fun pauseResume() = viewModelScope.launch {
        if (timer.state.value == ScrollState.RUNNING) timer.pause() else timer.resume()
    }
}
