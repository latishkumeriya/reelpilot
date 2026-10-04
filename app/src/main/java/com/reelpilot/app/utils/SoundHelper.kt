package com.reelpilot.app.utils

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Phase 5: short beep on auto-scroll. No audio files, no extra permissions.
 * ToneGenerator is process-wide; release only on process death (safe to keep).
 */
object SoundHelper {
    private var tone: ToneGenerator? = null

    fun beep() {
        try {
            if (tone == null) {
                tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            }
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        } catch (_: Exception) {
            try { tone?.release() } catch (_: Exception) {}
            tone = null
        }
    }
}
