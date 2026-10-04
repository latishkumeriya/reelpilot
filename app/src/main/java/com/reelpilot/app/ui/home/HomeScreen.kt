package com.reelpilot.app.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reelpilot.app.manager.ScrollState
import com.reelpilot.app.utils.PermissionUtils

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenInstagram: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumeTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val accessOk = remember(resumeTick) { PermissionUtils.isAccessibilityEnabled(ctx) }
    val overlayOk = remember(resumeTick) { PermissionUtils.canDrawOverlays(ctx) }

    val interval by vm.interval.collectAsState()
    val state by vm.state.collectAsState()
    val remaining by vm.remaining.collectAsState()
    val reels by vm.reels.collectAsState()
    val autoStart by vm.autoStart.collectAsState()
    val reset by vm.resetOnManual.collectAsState()
    val vibrate by vm.vibrate.collectAsState()
    val pauseReason by vm.pauseReason.collectAsState()
    val instaFg by vm.instagramForeground.collectAsState()
    val fgPkg by vm.foregroundPackage.collectAsState()
    val maxReels by vm.maxReels.collectAsState()
    val totalReels by vm.totalReels.collectAsState()

    var customText by remember(interval) { mutableStateOf(interval.toString()) }
    var showCustom by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        if (!accessOk) {
            WarningBanner("Accessibility OFF — auto-swipe won't work.") {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        if (!overlayOk) {
            WarningBanner("Overlay OFF — countdown bubble hidden.") {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (state) {
                        ScrollState.IDLE -> "IDLE"
                        ScrollState.RUNNING -> "RUNNING — ${remaining}s"
                        ScrollState.PAUSED -> "PAUSED — ${remaining}s left"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                if (state == ScrollState.PAUSED && pauseReason != null) {
                    Text(pauseReason!!, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (state != ScrollState.IDLE && autoStart) {
                    Text(
                        if (instaFg) "● Instagram detected"
                        else if (fgPkg != null) "○ In ${com.reelpilot.app.manager.AppDetector.label(fgPkg)} — waiting for target"
                        else "○ Waiting for target app…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (maxReels > 0) "Session: $reels / $maxReels  •  Lifetime: $totalReels"
                    else "Session: $reels  •  Lifetime: $totalReels",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Scroll every:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(35, 40, 60).forEach { preset ->
                FilterChip(
                    selected = interval == preset && !showCustom,
                    onClick = { showCustom = false; vm.setInterval(preset) },
                    label = { Text("${preset}s") }
                )
            }
            FilterChip(selected = showCustom, onClick = { showCustom = true }, label = { Text("Custom") })
        }

        if (showCustom) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = (customText.toIntOrNull() ?: 35).toFloat(),
                    onValueChange = { customText = it.toInt().toString() },
                    valueRange = 5f..300f,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.filter(Char::isDigit).take(3) },
                    label = { Text("sec") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp),
                    singleLine = true
                )
                Button(onClick = {
                    customText.toIntOrNull()?.let { vm.setInterval(it.coerceIn(5, 300)) }
                }) { Text("Set") }
            }
            Text("Allowed: 5–300 seconds. Applies to next cycle.", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Pause until Instagram open"); Switch(checked = autoStart, onCheckedChange = vm::setAutoStart)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reset timer on manual swipe"); Switch(checked = reset, onCheckedChange = vm::setReset)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Vibrate on scroll"); Switch(checked = vibrate, onCheckedChange = vm::setVibrate)
        }

        Spacer(Modifier.height(8.dp))

        if (state == ScrollState.IDLE) {
            Button(
                onClick = onStartService,
                enabled = accessOk,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(if (accessOk) "Start Auto-Scroll (${interval}s)" else "Enable Accessibility First") }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::pauseResume, modifier = Modifier.weight(1f)) {
                    Text(if (state == ScrollState.RUNNING) "Pause" else "Resume")
                }
                Button(onClick = { vm.stop(); onStopService() }, modifier = Modifier.weight(1f)) {
                    Text("Stop")
                }
            }
        }
        OutlinedButton(onClick = onOpenInstagram, modifier = Modifier.fillMaxWidth()) {
            Text("Open Instagram Reels")
        }
    }
}

@Composable
private fun WarningBanner(text: String, onFix: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}
