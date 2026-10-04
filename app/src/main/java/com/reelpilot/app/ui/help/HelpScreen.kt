package com.reelpilot.app.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How to use", style = MaterialTheme.typography.headlineMedium)
        listOf(
            "1. Grant Accessibility + Overlay." to "Without these Android blocks auto-swipe and the bubble.",
            "2. Pick 35s / 40s / 60s or Custom 5–300s." to "Timer resets when you swipe manually (if enabled).",
            "3. Tap Start, open Reels / Shorts / TikTok." to "Bubble ring counts down. ⏸ pause • +10s snooze • ⏹ stop.",
            "Bubble too big / transparent?" to "Settings → Bubble appearance: size 80–130%, opacity 40–100%, live.",
            "Stop after N reels?" to "Settings → Auto-stop: 0 = unlimited, up to 200. Service stops itself at the limit.",
            "Sound / vibration?" to "Settings → Behavior: beep (ToneGenerator) and/or 40ms vibration per scroll.",
            "Reboot resumes?" to "Settings → Restart on boot: resumes only if a session was active. On Android 12+ you may get a tap-to-resume notification instead.",
            "Will Instagram ban me?" to "No login, no API. It only sees normal swipes.",
            "Battery drain?" to "Minimal — 1 gesture per interval + low-priority notification. Unrestrict battery + autostart on Xiaomi/Oppo/Vivo."
        ).forEach { (q, a) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(q, style = MaterialTheme.typography.titleSmall)
                    Text(a, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
