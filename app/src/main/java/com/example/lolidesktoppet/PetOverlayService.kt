package com.example.lolidesktoppet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class PetOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var petView: View? = null
    private var bubble: TextView? = null
    private var bubbleVisible = true
    private var tapCount = 0
    private var phraseIndex = 0

    private val phrases = listOf(
        "主人～摸摸头嘛 ♡",
        "今天也要元气满满哦！",
        "欸嘿，被主人发现啦～",
        "抱抱！充电完成 ୨୧",
        "主人忙的话，我乖乖陪着你。"
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showPet()
    }

    private fun startAsForeground() {
        val channelId = "loli_pet_running"
        val channel = NotificationChannel(channelId, "桌宠运行状态", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("甜心桌宠正在陪伴你")
            .setContentText("回到应用可管理桌宠")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setOngoing(true)
            .build()
        startForeground(17, notification)
    }

    private fun showPet() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
        }
        bubble = TextView(this).apply {
            text = "主人～我来啦！♡"
            textSize = 13f
            setTextColor(Color.rgb(116, 75, 97))
            gravity = Gravity.CENTER
            setPadding(14, 9, 14, 9)
            background = rounded(Color.WHITE, 0xFFFFB8D2.toInt(), 28f)
        }
        val character = PetCharacterView(this)
        container.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = 2 })
        container.addView(character, LinearLayout.LayoutParams(112, 132))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 260
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) + abs(dy) > 8) dragged = true
                    params.x = startX + dx
                    params.y = startY + dy
                    try { wm.updateViewLayout(container, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        tapCount++
                        if (tapCount % 2 == 0) {
                            bubbleVisible = !bubbleVisible
                            bubble?.visibility = if (bubbleVisible) View.VISIBLE else View.GONE
                        } else {
                            bubbleVisible = true
                            bubble?.visibility = View.VISIBLE
                            bubble?.text = phrases[phraseIndex % phrases.size]
                            phraseIndex++
                            character.performClick()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        petView = container
        try {
            wm.addView(container, params)
        } catch (_: Exception) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun rounded(fill: Int, stroke: Int, radius: Float) = GradientDrawable().apply {
        setColor(fill)
        setCornerRadius(radius)
        setStroke(2, stroke)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        petView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        petView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
