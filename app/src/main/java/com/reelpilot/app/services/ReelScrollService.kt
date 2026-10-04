package com.reelpilot.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.manager.AppDetector
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.manager.ScrollTimerManager
import com.reelpilot.app.manager.SessionCoordinator
import com.reelpilot.app.utils.SoundHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ReelScrollService : AccessibilityService() {

    @Inject lateinit var timer: ScrollTimerManager
    @Inject lateinit var prefs: PrefsRepository
    @Inject lateinit var coordinator: SessionCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastAutoSwipe = 0L
    private var lastPackage: String? = null

    companion object {
        var instance: ReelScrollService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
        timer.onScrollTick = {
            scope.launch { gatedSwipe() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString()

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg != null) {
            if (pkg != lastPackage) {
                lastPackage = pkg
                coordinator.setForegroundPackage(pkg)
                coordinator.setInstagramForeground(pkg == AppDetector.INSTAGRAM_PKG)
            }
        }

        // Manual-swipe reset: only for supported short-video apps
        if (pkg != null && AppDetector.ALL.contains(pkg) &&
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            val now = System.currentTimeMillis()
            if (now - lastAutoSwipe > 2500) {
                scope.launch { timer.notifyManualScroll() }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (instance == this) instance = null
    }

    private suspend fun gatedSwipe() {
        if (timer.state.value != ScrollState.RUNNING) return
        val autoStart = prefs.autoStart.first()
        if (autoStart) {
            val enabled = mutableSetOf<String>().apply {
                if (prefs.enableInstagram.first()) add(AppDetector.INSTAGRAM_PKG)
                if (prefs.enableYoutube.first()) add(AppDetector.YOUTUBE_PKG)
                if (prefs.enableTiktok.first()) {
                    add(AppDetector.TIKTOK_PKG); add(AppDetector.TIKTOK_LITE_PKG)
                }
            }
            val cur = coordinator.foregroundPackage.value
            if (cur == null || !enabled.contains(cur)) return
        }

        performReelSwipe()

        if (prefs.vibrateOnScroll.first()) {
            vibrateShort()
        }
        try {
            if (prefs.soundOnScroll.first()) SoundHelper.beep()
        } catch (_: Exception) {}
    }

    fun performReelSwipe() {
        if (Build.VERSION.SDK_INT < 24) return
        val wm = getSystemService(WindowManager::class.java) ?: return
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getMetrics(metrics)
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.5f, h * 0.28f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        lastAutoSwipe = System.currentTimeMillis()
        dispatchGesture(gesture, null, null)
    }

    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") v.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }
}
