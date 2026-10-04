package com.reelpilot.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reelpilot.app.services.ScrollForegroundService
import com.reelpilot.app.ui.help.HelpScreen
import com.reelpilot.app.ui.home.HomeScreen
import com.reelpilot.app.ui.onboarding.OnboardingScreen
import com.reelpilot.app.ui.settings.SettingsScreen
import com.reelpilot.app.ui.theme.ReelPilotTheme
import com.reelpilot.app.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            ReelPilotTheme {
                var tab by remember { mutableIntStateOf(0) }
                var onboarded by remember { mutableStateOf(checkCorePermissions()) }
                val lifecycle = LocalLifecycleOwner.current.lifecycle

                // Re-check when returning from Settings — auto-advance if done
                DisposableEffect(lifecycle) {
                    val obs = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME && !onboarded) {
                            if (checkCorePermissions()) onboarded = true
                        }
                    }
                    lifecycle.addObserver(obs)
                    onDispose { lifecycle.removeObserver(obs) }
                }

                if (!onboarded) {
                    OnboardingScreen(onDone = { onboarded = true })
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                                    icon = { Icon(Icons.Default.Info, null) }, label = { Text("Help") })
                            }
                        }
                    ) { pad ->
                        androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                            when (tab) {
                                0 -> HomeScreen(
                                    onStartService = { startScrollService() },
                                    onStopService = { stopScrollService() },
                                    onOpenInstagram = { openInstagram() }
                                )
                                1 -> SettingsScreen()
                                else -> HelpScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkCorePermissions(): Boolean {
        return try {
            PermissionUtils.isAccessibilityEnabled(this) && PermissionUtils.canDrawOverlays(this)
        } catch (_: Exception) { false }
    }

    private fun startScrollService() {
        if (!PermissionUtils.isAccessibilityEnabled(this)) return
        startForegroundService(Intent(this, ScrollForegroundService::class.java))
    }

    private fun stopScrollService() {
        // Tell service to shut down cleanly (it stops timer + hides bubble itself)
        startService(Intent(this, ScrollForegroundService::class.java).setAction(ScrollForegroundService.ACTION_STOP))
    }

    private fun openInstagram() {
        try {
            val launch = packageManager.getLaunchIntentForPackage("com.instagram.android")
            if (launch != null) startActivity(launch)
            else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/")))
        }
    }
}
