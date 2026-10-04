package com.reelpilot.app.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.reelpilot.app.MainActivity
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.manager.ScrollTimerManager
import com.reelpilot.app.manager.SessionCoordinator
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.overlay.FloatingBubbleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class ScrollForegroundService : LifecycleService() {

    @Inject lateinit var timer: ScrollTimerManager
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var bubble: FloatingBubbleManager
    @Inject lateinit var prefs: PrefsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null

    companion object {
        const val CHANNEL_ID = "reelpilot_running"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.reelpilot.app.STOP"
        const val ACTION_PAUSE = "com.reelpilot.app.PAUSE"
        const val ACTION_RESUME = "com.reelpilot.app.RESUME"
        const val ACTION_SNOOZE = "com.reelpilot.app.SNOOZE"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        coordinator.setUserSessionActive(true)
        scope.launch { try { prefs.setSessionWasActive(true) } catch (_: Exception) {} }
        timer.onMaxReached = { scope.launch { shutdown() } }
        scope.launch { timer.start() }
        // Phase 4: Compose bubble needs a LifecycleOwner — this LifecycleService is one.
        try { bubble.show(this@ScrollForegroundService) { scope.launch { shutdown() } } } catch (_: Exception) {}
        registerScreenReceiver()

        collectJob = scope.launch {
            launch { timer.state.collect { updateNotification() } }
            launch { timer.remaining.collect { updateNotification() } }
            launch { coordinator.pauseReason.collect { updateNotification() } }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { shutdown() }
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                scope.launch { timer.pause() }
                return START_STICKY
            }
            ACTION_RESUME -> {
                scope.launch { timer.resume() }
                return START_STICKY
            }
            ACTION_SNOOZE -> {
                scope.launch { timer.addSeconds(10) }
                return START_STICKY
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, buildNotif(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, buildNotif())
        }
        return START_STICKY
    }

    override fun onDestroy() {
        shutdownInternal()
        super.onDestroy()
    }

    private suspend fun shutdown() {
        try { timer.stop() } catch (_: Exception) {}
        shutdownInternal()
        stopSelf()
    }

    private fun shutdownInternal() {
        try { coordinator.setUserSessionActive(false) } catch (_: Exception) {}
        try { timer.onMaxReached = null } catch (_: Exception) {}
        try { bubble.hide() } catch (_: Exception) {}
        // Clear boot-resume flag (fire-and-forget; scope may already be cancelled)
        try { kotlinx.coroutines.GlobalScope.launch { try { prefs.setSessionWasActive(false) } catch (_: Exception) {} } } catch (_: Exception) {}
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        screenReceiver = null
        collectJob?.cancel()
        scope.cancel()
    }

    private fun registerScreenReceiver() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> coordinator.setScreenOn(false)
                    Intent.ACTION_SCREEN_ON -> coordinator.setScreenOn(true)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(r, filter)
        }
        screenReceiver = r
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.notify(NOTIF_ID, buildNotif())
        } catch (_: Exception) {}
    }

    private fun buildNotif(): Notification {
        val state = try { timer.state.value } catch (_: Exception) { ScrollState.IDLE }
        val remaining = try { timer.remaining.value } catch (_: Exception) { 0 }
        val reels = try { timer.reelsScrolled.value } catch (_: Exception) { 0 }
        val reason = try { coordinator.pauseReason.value } catch (_: Exception) { null }

        val title = when (state) {
            ScrollState.RUNNING -> "ReelPilot • $remaining s — $reels reels"
            ScrollState.PAUSED -> reason ?: "ReelPilot • paused ($remaining s left)"
            ScrollState.IDLE -> "ReelPilot • stopped"
        }

        val openIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Bubble: ⏸ pause • +10s snooze • ⏹ stop")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (state == ScrollState.RUNNING) {
            val pi = android.app.PendingIntent.getService(
                this, 10, Intent(this, ScrollForegroundService::class.java).setAction(ACTION_PAUSE),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pi)
            val snooze = android.app.PendingIntent.getService(
                this, 13, Intent(this, ScrollForegroundService::class.java).setAction(ACTION_SNOOZE),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_recent_history, "+10s", snooze)
        } else if (state == ScrollState.PAUSED) {
            val pi = android.app.PendingIntent.getService(
                this, 11, Intent(this, ScrollForegroundService::class.java).setAction(ACTION_RESUME),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pi)
        }
        val stopPi = android.app.PendingIntent.getService(
            this, 12, Intent(this, ScrollForegroundService::class.java).setAction(ACTION_STOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopPi)
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "Auto-scroll status", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }
}
