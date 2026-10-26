package com.example.lolidesktoppet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color

class MainActivity : Activity() {
    private val pink = Color.rgb(255, 128, 174)
    private val ink = Color.rgb(76, 56, 69)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 34, 28, 28)
            setBackgroundColor(Color.rgb(255, 246, 250))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "♡ 甜心桌宠 ♡"
            textSize = 30f
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 8)
        }
        val subtitle = TextView(this).apply {
            text = "粉白甜系 · 软萌撒娇 · 随时陪伴"
            textSize = 15f
            setTextColor(Color.rgb(177, 112, 143))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        val mascot = TextView(this).apply {
            text = "૮₍ ˶ᵔ ᵕ ᵔ˶ ₎ა\n🎀 ୨୧ 🎀"
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(pink)
            setPadding(0, 16, 0, 18)
        }
        val desc = TextView(this).apply {
            text = "“主人～今天也要开心哦！”\n\n启动后，她会出现在其他应用上方。可以拖动她、点击听她撒娇，连续点击可收起互动气泡。"
            textSize = 16f
            setTextColor(ink)
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setPadding(8, 0, 8, 22)
        }
        val permission = Button(this).apply {
            text = "① 开启悬浮窗权限"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else startPet()
            }
        }
        val start = Button(this).apply {
            text = "② 启动桌面宠物"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) startPet()
                else {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
            }
        }
        val stop = Button(this).apply {
            text = "收起桌宠"
            setOnClickListener { stopService(Intent(this@MainActivity, PetOverlayService::class.java)) }
        }
        listOf(permission, start, stop).forEach {
            it.textSize = 16f
            it.setTextColor(Color.WHITE)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(pink)
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(mascot)
        content.addView(desc)
        listOf(permission, start, stop).forEach {
            val lp = LinearLayout.LayoutParams(-1, 54)
            lp.setMargins(0, 7, 0, 7)
            content.addView(it, lp)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun startPet() {
        val intent = Intent(this, PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        finish()
    }
}
