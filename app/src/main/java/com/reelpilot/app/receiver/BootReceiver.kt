package com.reelpilot.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reelpilot.app.MainActivity
import com.reelpilot.app.data.PrefsRepository
import com.reelpilot.app.services.ScrollForegroundService
import com.reelpilot.app.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 5: resume prompt / auto-resume after reboot.
 * Only acts if user enabled "Restart on boot" AND left a session active.
 * On Android 12+ a background FGS start may be denied → falls back to tap-to-resume notification.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: PrefsRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                val wantRestart = try { prefs.restartOnBoot.first() } catch (_: Exception) { false }
                val wasActive = try { prefs.sessionWasActive.first() } catch (_: Exception) { false }
                if (!wantRestart || !wasActive) return@launch
                if (!PermissionUtils.isAccessibilityEnabled(context)) {
                    showResumePrompt(context)
                    return@launch
                }
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(Intent(context, ScrollForegroundService::class.java))
                    } else {
                        context.startService(Intent(context, ScrollForegroundService::class.java))
                    }
                } catch (_: Exception) {
                    showResumePrompt(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showResumePrompt(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel("reelpilot_boot", "Resume prompts", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, "reelpilot_boot")
                .setContentTitle("ReelPilot: tap to resume auto-scroll")
                .setContentText("Your session was active before reboot.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            nm.notify(2002, notif)
        } catch (_: Exception) {}
    }
}
