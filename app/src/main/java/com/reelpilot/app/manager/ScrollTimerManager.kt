package com.reelpilot.app.manager

import com.reelpilot.app.data.PrefsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class ScrollState { IDLE, RUNNING, PAUSED }

@Singleton
class ScrollTimerManager @Inject constructor(
    private val prefs: PrefsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ScrollState.IDLE)
    val state: StateFlow<ScrollState> = _state

    private val _remaining = MutableStateFlow(PrefsRepository.DEFAULT_INTERVAL)
    val remaining: StateFlow<Int> = _remaining

    private val _reelsScrolled = MutableStateFlow(0)
    val reelsScrolled: StateFlow<Int> = _reelsScrolled

    /** True if last pause came from SessionCoordinator (screen off / app switch). */
    var wasPausedBySystem: Boolean = false
        private set

    var onScrollTick: (() -> Unit)? = null
    /** Fired when max-reels limit is hit so the service can shut down cleanly. */
    var onMaxReached: (() -> Unit)? = null

    private var job: Job? = null

    suspend fun start() {
        val interval = prefs.intervalSec.first()
        _remaining.value = interval
        _reelsScrolled.value = 0
        wasPausedBySystem = false
        _state.value = ScrollState.RUNNING
        try { prefs.recordSession() } catch (_: Exception) {}
        startLoop()
    }

    /** User pressed Pause */
    fun pause() {
        wasPausedBySystem = false
        _state.value = ScrollState.PAUSED
        job?.cancel()
    }

    /** System pressed pause (screen off, left target app). Auto-resumable. */
    fun pauseBySystem() {
        if (_state.value != ScrollState.RUNNING) return
        wasPausedBySystem = true
        _state.value = ScrollState.PAUSED
        job?.cancel()
    }

    suspend fun resume() {
        if (_state.value != ScrollState.PAUSED) return
        wasPausedBySystem = false
        _state.value = ScrollState.RUNNING
        startLoop(keepRemaining = true)
    }

    suspend fun resumeInternal() {
        if (_state.value != ScrollState.PAUSED || !wasPausedBySystem) return
        wasPausedBySystem = false
        _state.value = ScrollState.RUNNING
        startLoop(keepRemaining = true)
    }

    fun stop() {
        wasPausedBySystem = false
        _state.value = ScrollState.IDLE
        job?.cancel()
    }

    suspend fun resetToFull() {
        _remaining.value = prefs.intervalSec.first()
    }

    /** Bubble "+10s" — postpone next scroll. Clamped to interval + 120s. */
    suspend fun addSeconds(sec: Int) {
        val cap = prefs.intervalSec.first() + 120
        _remaining.value = (_remaining.value + sec).coerceIn(1, cap)
    }

    /** Called when AccessibilityService sees a manual swipe */
    suspend fun notifyManualScroll() {
        val reset = prefs.resetOnManual.first()
        if (reset && _state.value == ScrollState.RUNNING) resetToFull()
    }

    private fun startLoop(keepRemaining: Boolean = false) {
        job?.cancel()
        if (!keepRemaining || _remaining.value <= 0) {
            scope.launch { _remaining.value = prefs.intervalSec.first() }
        }
        job = scope.launch {
            while (isActive && _state.value == ScrollState.RUNNING) {
                delay(1000)
                if (_state.value != ScrollState.RUNNING) break
                _remaining.value -= 1
                if (_remaining.value <= 0) {
                    try { onScrollTick?.invoke() } catch (_: Exception) {}
                    _reelsScrolled.value += 1
                    try { prefs.recordScroll() } catch (_: Exception) {}
                    val max = prefs.maxReels.first()
                    if (max > 0 && _reelsScrolled.value >= max) {
                        stop()
                        try { onMaxReached?.invoke() } catch (_: Exception) {}
                        break
                    }
                    _remaining.value = prefs.intervalSec.first()
                }
            }
        }
    }
}
