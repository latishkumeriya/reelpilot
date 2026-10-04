package com.reelpilot.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Xiaomi / Oppo / Vivo / Samsung kill foreground services aggressively.
 * Best-effort deep-link into OEM autostart screens. Safe to fail silently.
 */
object AutostartHelper {

    private val OEM_INTENTS: List<Intent> = listOf(
        // Xiaomi
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        // Oppo
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        // Vivo
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        // Samsung
        Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
        // Huawei
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
    )

    val manufacturer: String = Build.MANUFACTURER ?: "unknown"

    /** @return true if an OEM settings screen was opened */
    fun openAutostartSettings(context: Context): Boolean {
        for (base in OEM_INTENTS) {
            try {
                context.startActivity(base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) { /* try next */ }
        }
        return false
    }

    fun needsAutostartPrompt(): Boolean {
        val m = manufacturer.lowercase()
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ||
                m.contains("oppo") || m.contains("realme") || m.contains("vivo") ||
                m.contains("oneplus") || m.contains("huawei")
    }
}
