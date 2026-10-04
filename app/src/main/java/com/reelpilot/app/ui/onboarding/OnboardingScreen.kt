package com.reelpilot.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reelpilot.app.utils.AutostartHelper
import com.reelpilot.app.utils.BatteryUtils
import com.reelpilot.app.utils.PermissionUtils

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
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

    // Live status — re-evaluated on every resume
    val accessOk = remember(resumeTick) { PermissionUtils.isAccessibilityEnabled(ctx) }
    val overlayOk = remember(resumeTick) { PermissionUtils.canDrawOverlays(ctx) }
    val batteryOk = remember(resumeTick) { BatteryUtils.isIgnoringOptimizations(ctx) }
    val allCore = accessOk && overlayOk

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Setup permissions", style = MaterialTheme.typography.headlineMedium)
        Text("ReelPilot needs OS access to auto-scroll. No data leaves your phone.")

        PermissionRow(
            title = "1. Accessibility Service",
            desc = if (accessOk) "✓ Enabled" else "Detect Reels + perform swipe",
            done = accessOk,
            button = if (accessOk) "Open" else "Enable",
            onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )
        PermissionRow(
            title = "2. Display over other apps",
            desc = if (overlayOk) "✓ Allowed" else "Show countdown bubble on Instagram",
            done = overlayOk,
            button = if (overlayOk) "Open" else "Allow",
            onClick = {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                )
            }
        )
        PermissionRow(
            title = "3. Ignore battery optimizations",
            desc = if (batteryOk) "✓ Unrestricted" else "Prevents ${AutostartHelper.manufacturer} from killing timer",
            done = batteryOk,
            button = "Fix",
            onClick = { BatteryUtils.requestIgnoreOptimizations(ctx) }
        )

        if (AutostartHelper.needsAutostartPrompt()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("4. Autostart (${AutostartHelper.manufacturer})", style = MaterialTheme.typography.titleSmall)
                        Text("Required on Xiaomi/Oppo/Vivo or timer stops in background",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { AutostartHelper.openAutostartSettings(ctx) }) { Text("Open") }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (!allCore) {
            Text("Enable 1 + 2 to continue. Return here after each settings screen — status updates automatically.",
                style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onDone,
            enabled = allCore,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (allCore) "Done — Go to Home" else "Complete steps above")
        }
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, done: Boolean, button: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.cardColors()
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClick) { Text(if (done) "✓" else button) }
        }
    }
}
