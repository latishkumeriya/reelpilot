package com.reelpilot.app.manager

/**
 * Phase 4: multi-app target detection.
 * Instagram Reels (default ON), YouTube Shorts + TikTok (opt-in in Settings).
 */
object AppDetector {
    const val INSTAGRAM_PKG = "com.instagram.android"
    const val YOUTUBE_PKG = "com.google.android.youtube"
    const val TIKTOK_PKG = "com.zhiliaoapp.musically"
    const val TIKTOK_LITE_PKG = "com.zhiliaoapp.musically.go"

    val ALL = listOf(INSTAGRAM_PKG, YOUTUBE_PKG, TIKTOK_PKG, TIKTOK_LITE_PKG)

    fun label(pkg: String?): String = when (pkg) {
        INSTAGRAM_PKG -> "Instagram"
        YOUTUBE_PKG -> "YouTube"
        TIKTOK_PKG, TIKTOK_LITE_PKG -> "TikTok"
        else -> pkg ?: "other app"
    }

    fun isShortVideoScreen(pkg: String?, className: CharSequence?): Boolean {
        if (pkg == null) return false
        val cls = className?.toString()?.lowercase() ?: ""
        return when (pkg) {
            INSTAGRAM_PKG -> cls.contains("reel") || cls.contains("clips") || cls.contains("video") || cls.isEmpty()
            YOUTUBE_PKG -> cls.contains("short") || cls.contains("reel") || cls.contains("watch") || cls.isEmpty()
            TIKTOK_PKG, TIKTOK_LITE_PKG -> true // TikTok is near-always vertical video
            else -> false
        }
    }
}
