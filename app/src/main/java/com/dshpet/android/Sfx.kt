package com.dshpet.android

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Lightweight tap feedback.
 *
 * The repository does not currently contain the tap_press/tap_release audio files
 * referenced by the previous SoundPool implementation. Use Android's built-in
 * tone generator instead so the APK has no missing raw-resource dependency.
 */
object Sfx {
    private var toneGenerator: ToneGenerator? = null

    /** Master switch controlled by the settings page. */
    @Volatile
    var enabled: Boolean = true

    fun init(ctx: Context) {
        if (toneGenerator != null) return
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
        } catch (t: Throwable) {
            Diag.record(ctx, "音效初始化失败: ${t.message}")
        }
    }

    /** Touch-down feedback. */
    fun press() = play(ToneGenerator.TONE_PROP_BEEP)

    /** Touch-up feedback. */
    fun release() = play(ToneGenerator.TONE_PROP_ACK)

    private fun play(tone: Int) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(tone, 45)
        } catch (_: Throwable) {
            // Sound must never interfere with pet interaction.
        }
    }
}
