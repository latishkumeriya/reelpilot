package com.reelpilot.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewtree.ViewTreeLifecycleOwner
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.manager.ScrollTimerManager
import com.reelpilot.app.manager.SessionCoordinator
import com.reelpilot.app.ui.theme.ReelPilotTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: Compose overlay bubble with countdown ring + Pause/+10s/Stop.
 * Hosted in ScrollForegroundService (a LifecycleService) via ComposeView.
 */
@Singleton
class FloatingBubbleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timer: ScrollTimerManager,
    private val prefs: PrefsRepository,
    private val coordinator: SessionCoordinator
) {
    private var view: ComposeView? = null
    private var onStopRequest: (() -> Unit)? = null
    private var appearanceJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val isShowing: Boolean get() = view != null

    @SuppressLint("ClickableViewAccessibility")
    fun show(owner: LifecycleOwner, onStop: () -> Unit) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) return
        onStopRequest = onStop
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val composeView = ComposeView(context).apply {
            setContent {
                ReelPilotTheme {
                    val remaining by timer.remaining.collectAsState()
                    val total by prefs.intervalSec.collectAsState(initial = 35)
                    val state by timer.state.collectAsState()
                    val reason by coordinator.pauseReason.collectAsState()
                    ReelBubble(
                        remaining = remaining,
                        total = total,
                        state = state,
                        pauseReason = reason,
                        onPauseResume = {
                            scope.launch {
                                if (timer.state.value == ScrollState.RUNNING) timer.pause()
                                else timer.resume()
                            }
                        },
                        onSnooze = { scope.launch { timer.addSeconds(10) } },
                        onStop = { onStopRequest?.invoke() }
                    )
                }
            }
        }
        ViewTreeLifecycleOwner.set(composeView, owner)

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; y = 220; x = 16 }

        // Drag-to-move: long-press-drag moves, short tap passes through to Compose buttons
        var downX = 0; var downY = 0
        var moved = false
        composeView.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX.toInt(); downY = e.rawY.toInt(); moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX.toInt() - downX
                    val dy = e.rawY.toInt() - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 16) moved = true
                    if (moved) {
                        params.x -= dx; params.y += dy
                        try { wm.updateViewLayout(composeView, params) } catch (_: Exception) {}
                        downX = e.rawX.toInt(); downY = e.rawY.toInt()
                    }
                }
            }
            // Consume move-to-drag, let clicks reach Compose buttons
            moved
        }

        view = composeView
        try { wm.addView(composeView, params) } catch (_: Exception) { view = null; return }
        // Phase 5: apply saved size/opacity + follow live changes from Settings
        appearanceJob?.cancel()
        appearanceJob = scope.launch {
            launch {
                try { prefs.overlayScale.first() } catch (_: Exception) { 1f }.let { s ->
                    composeView.scaleX = s; composeView.scaleY = s
                }
                prefs.overlayScale.collect { s ->
                    try { composeView.scaleX = s; composeView.scaleY = s } catch (_: Exception) {}
                }
            }
            launch {
                try { prefs.overlayAlpha.first() } catch (_: Exception) { 0.94f }.let { a ->
                    composeView.alpha = a
                }
                prefs.overlayAlpha.collect { a ->
                    try { composeView.alpha = a } catch (_: Exception) {}
                }
            }
        }
    }

    /** Legacy no-arg show kept for compat — no-op without a LifecycleOwner. */
    fun show() { /* use show(owner, onStop) from the foreground service */ }

    fun hide() {
        appearanceJob?.cancel()
        appearanceJob = null
        view?.let {
            try { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it) }
            catch (_: Exception) {}
        }
        view = null
        onStopRequest = null
    }

    fun destroy() {
        hide()
        scope.cancel()
    }
}
