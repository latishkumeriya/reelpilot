package com.reelpilot.app.manager

object InstagramDetector {
    const val INSTAGRAM_PKG = "com.instagram.android"

    // Best-effort Reels heuristics. Falls back to package-only detection.
    fun isReelsScreen(className: CharSequence?, text: List<CharSequence>): Boolean {
        val cls = className?.toString()?.lowercase() ?: ""
        // ReelViewerFragment / ClipsViewer activity names seen across versions
        if (cls.contains("reel") || cls.contains("clips") || cls.contains("video")) return true
        for (t in text) {
            val s = t.toString()
            if (s.contains("Reels", ignoreCase = true)) return true
        }
        return false
    }
}
