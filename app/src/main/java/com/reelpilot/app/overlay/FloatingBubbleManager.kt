package com.reelpilot.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.manager.ScrollTimerManager
import com.reelpilot.app.manager.SessionCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 5: classic-Views overlay bubble (countdown + Pause/+10s/Stop).
 * Deliberately NOT Compose: a ComposeView inside a Service overlay requires
 * ViewTreeLifecycleOwner plumbing that needs extra artifacts. Plain Views work
 * everywhere with zero extra dependencies.
 */
@Singleton
class FloatingBubbleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timer: ScrollTimerManager,
    private val prefs: PrefsRepository,
    private val coordinator: SessionCoordinator
) {
    private var root: LinearLayout? = null
    private var countdownView: TextView? = null
    private var statusView: TextView? = null
    private var pauseButton: Button? = null
    private var onStopRequest: (() -> Unit)? = null
    private var uiJob: Job? = null
    private var appearanceJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val isShowing: Boolean get() = root != null

    @SuppressLint("ClickableViewAccessibility")
    fun show(onStop: () -> Unit) {
        if (root != null) return
        if (!Settings.canDrawOverlays(context)) return
        onStopRequest = onStop
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = context.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val countdown = TextView(context).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val status = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#FFAAAAAA"))
            gravity = Gravity.CENTER
        }
        val pause = Button(context).apply { text = "Pause" }
        val snooze = Button(context).apply { text = "+10s" }
        val stop = Button(context).apply { text = "Stop" }

        pause.setOnClickListener {
            scope.launch {
                if (timer.state.value == ScrollState.RUNNING) timer.pause()
                else timer.resume()
            }
        }
        snooze.setOnClickListener { scope.launch { timer.addSeconds(10) } }
        stop.setOnClickListener { onStopRequest?.invoke() }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(pause)
            addView(snooze)
            addView(stop)
        }
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC111111"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(countdown)
            addView(status)
            addView(row)
        }

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

        // Drag-to-move on the background; buttons still receive clicks
        var downX = 0; var downY = 0
        var moved = false
        layout.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX.toInt(); downY = e.rawY.toInt(); moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX.toInt() - downX
                    val dy = e.rawY.toInt() - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 16) moved = true
                    if (moved) {
                        params.x -= dx; params.y += dy
                        try { wm.updateViewLayout(layout, params) } catch (_: Exception) {}
                        downX = e.rawX.toInt(); downY = e.rawY.toInt()
                    }
                }
            }
            moved
        }

        countdownView = countdown
        statusView = status
        pauseButton = pause
        root = layout
        try {
            wm.addView(layout, params)
        } catch (_: Exception) {
            root = null; countdownView = null; statusView = null; pauseButton = null
            return
        }

        refresh()
        uiJob?.cancel()
        uiJob = scope.launch {
            launch { timer.remaining.collect { refresh() } }
            launch { timer.state.collect { refresh() } }
            launch { coordinator.pauseReason.collect { refresh() } }
        }
        // Phase 5: apply saved size/opacity + follow live changes from Settings
        appearanceJob?.cancel()
        appearanceJob = scope.launch {
            launch {
                try { prefs.overlayScale.first() } catch (_: Exception) { 1f }.let { s ->
                    try { layout.scaleX = s; layout.scaleY = s } catch (_: Exception) {}
                }
                prefs.overlayScale.collect { s ->
                    try { layout.scaleX = s; layout.scaleY = s } catch (_: Exception) {}
                }
            }
            launch {
                try { prefs.overlayAlpha.first() } catch (_: Exception) { 0.94f }.let { a ->
                    try { layout.alpha = a } catch (_: Exception) {}
                }
                prefs.overlayAlpha.collect { a ->
                    try { layout.alpha = a } catch (_: Exception) {}
                }
            }
        }
    }

    private fun refresh() {
        val remaining = try { timer.remaining.value } catch (_: Exception) { 0 }
        val state = try { timer.state.value } catch (_: Exception) { ScrollState.IDLE }
        val reason = try { coordinator.pauseReason.value } catch (_: Exception) { null }
        countdownView?.text = when (state) {
            ScrollState.RUNNING -> "${remaining}s"
            ScrollState.PAUSED -> "${remaining}s"
            ScrollState.IDLE -> "--"
        }
        statusView?.text = when (state) {
            ScrollState.RUNNING -> "next scroll"
            ScrollState.PAUSED -> (reason ?: "paused").take(24)
            ScrollState.IDLE -> "stopped"
        }
        pauseButton?.text = if (state == ScrollState.RUNNING) "Pause" else "Start"
    }

    fun hide() {
        uiJob?.cancel()
        uiJob = null
        appearanceJob?.cancel()
        appearanceJob = null
        root?.let {
            try { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it) }
            catch (_: Exception) {}
        }
        root = null
        countdownView = null
        statusView = null
        pauseButton = null
        onStopRequest = null
    }

    fun destroy() {
        hide()
        scope.cancel()
    }
}
