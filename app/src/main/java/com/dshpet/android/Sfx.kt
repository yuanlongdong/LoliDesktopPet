package com.dshpet.android

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * 点击音效：有可选的 tap_press / tap_release 素材时播放。
 * 素材不随仓库分发时保持静音，不让缺失的 R.raw 资源阻断 APK 编译。
 */
object Sfx {

    private var pool: SoundPool? = null
    private var pressId = 0
    private var releaseId = 0
    private var ready = false

    @Volatile
    var enabled: Boolean = true

    fun init(ctx: Context) {
        if (pool != null) return
        try {
            val pressRes = ctx.resources.getIdentifier("tap_press", "raw", ctx.packageName)
            val releaseRes = ctx.resources.getIdentifier("tap_release", "raw", ctx.packageName)
            if (pressRes == 0 && releaseRes == 0) {
                Diag.record(ctx, "未提供可选点击音效素材，点击音效保持静音")
                return
            }

            val p = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            p.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) ready = true
            }
            if (pressRes != 0) pressId = p.load(ctx, pressRes, 1)
            if (releaseRes != 0) releaseId = p.load(ctx, releaseRes, 1)
            pool = p
        } catch (t: Throwable) {
            Diag.record(ctx, "音效初始化失败: ${t.message}")
        }
    }

    fun press() = play(pressId)

    fun release() = play(releaseId)

    private fun play(id: Int) {
        if (!enabled || id == 0) return
        val p = pool ?: return
        if (!ready) return
        try {
            p.play(id, 0.85f, 0.85f, 1, 0, 1.0f)
        } catch (_: Throwable) {
            // 音效失败绝不影响交互
        }
    }
}
