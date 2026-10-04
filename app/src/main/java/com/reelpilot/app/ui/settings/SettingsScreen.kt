package com.reelpilot.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reelpilot.app.ui.home.HomeViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: HomeViewModel = hiltViewModel()) {
    val vibrate by vm.vibrate.collectAsState()
    val autoStart by vm.autoStart.collectAsState()
    val reset by vm.resetOnManual.collectAsState()
    val maxReels by vm.maxReels.collectAsState()
    val enInsta by vm.enableInstagram.collectAsState()
    val enYt by vm.enableYoutube.collectAsState()
    val enTik by vm.enableTiktok.collectAsState()
    val totalReels by vm.totalReels.collectAsState()
    val totalSessions by vm.totalSessions.collectAsState()
    val sound by vm.sound.collectAsState()
    val restartOnBoot by vm.restartOnBoot.collectAsState()
    val scale by vm.overlayScale.collectAsState()
    val alpha by vm.overlayAlpha.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target apps", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Instagram Reels"); Switch(enInsta, vm::setEnableInstagram)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("YouTube Shorts"); Switch(enYt, vm::setEnableYoutube)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TikTok"); Switch(enTik, vm::setEnableTiktok)
                }
                if (!enInsta && !enYt && !enTik) {
                    Text("⚠ Select at least one app.", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto-stop", style = MaterialTheme.typography.titleSmall)
                Text(if (maxReels == 0) "Unlimited" else "Stop after $maxReels reels")
                Slider(
                    value = maxReels.toFloat(),
                    onValueChange = { vm.setMaxReels(it.toInt()) },
                    valueRange = 0f..200f,
                    steps = 19
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 25, 50, 100).forEach { preset ->
                        FilterChip(
                            selected = maxReels == preset,
                            onClick = { vm.setMaxReels(preset) },
                            label = { Text(if (preset == 0) "∞" else "$preset") }
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bubble appearance (live)", style = MaterialTheme.typography.titleSmall)
                Text("Size: ${(scale * 100).roundToInt()}%")
                Slider(value = scale, onValueChange = vm::setOverlayScale, valueRange = 0.8f..1.3f)
                Text("Opacity: ${(alpha * 100).roundToInt()}%")
                Slider(value = alpha, onValueChange = vm::setOverlayAlpha, valueRange = 0.4f..1f)
                Text("Start a session to preview — changes apply instantly.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Behavior", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pause until target app"); Switch(autoStart, vm::setAutoStart)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reset on manual swipe"); Switch(reset, vm::setReset)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibrate on scroll"); Switch(vibrate, vm::setVibrate)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Beep on scroll"); Switch(sound, vm::setSound)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Restart on boot")
                        Text("Resume if session was active", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(restartOnBoot, vm::setRestartOnBoot)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lifetime stats", style = MaterialTheme.typography.titleSmall)
                Text("Total reels auto-scrolled: $totalReels")
                Text("Total sessions: $totalSessions")
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = vm::resetStats) { Text("Reset stats") }
            }
        }
    }
}
