package com.example.lolidesktoppet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

/** Simplified vector mascot placeholder for the first APK prototype. */
class PetCharacterView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pink = Color.rgb(255, 153, 190)
    private val deepPink = Color.rgb(222, 104, 153)
    private val cream = Color.rgb(255, 247, 250)
    private val hair = Color.rgb(255, 231, 239)
    private val line = Color.rgb(133, 91, 112)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = min(width / 112f, height / 132f)
        canvas.save()
        canvas.scale(s, s)

        paint.color = 0x227D4A63
        canvas.drawOval(17f, 113f, 95f, 128f, paint)
        paint.color = hair
        canvas.drawOval(7f, 27f, 37f, 82f, paint)
        canvas.drawOval(75f, 27f, 105f, 82f, paint)
        paint.color = pink
        canvas.drawOval(9f, 49f, 31f, 82f, paint)
        canvas.drawOval(81f, 49f, 103f, 82f, paint)
        paint.color = hair
        canvas.drawOval(18f, 12f, 94f, 89f, paint)
        paint.color = cream
        canvas.drawOval(23f, 25f, 89f, 84f, paint)

        paint.color = hair
        val bangs = Path().apply {
            moveTo(20f, 39f); quadTo(22f, 9f, 55f, 15f)
            quadTo(91f, 11f, 92f, 40f); quadTo(75f, 31f, 67f, 43f)
            quadTo(53f, 30f, 42f, 45f); quadTo(30f, 36f, 20f, 48f); close()
        }
        canvas.drawPath(bangs, paint)
        paint.color = 0x66FF8EB9
        canvas.drawOval(27f, 54f, 42f, 62f, paint)
        canvas.drawOval(70f, 54f, 85f, 62f, paint)
        paint.color = deepPink
        canvas.drawOval(36f, 47f, 45f, 60f, paint)
        canvas.drawOval(67f, 47f, 76f, 60f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(40f, 50f, 2.3f, paint)
        canvas.drawCircle(71f, 50f, 2.3f, paint)

        paint.color = line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        canvas.drawArc(49f, 59f, 62f, 68f, 15f, 150f, false, paint)
        paint.style = Paint.Style.FILL

        paint.color = pink
        val bow = Path().apply {
            moveTo(53f, 19f); quadTo(36f, 7f, 37f, 20f)
            quadTo(42f, 27f, 53f, 21f); close()
        }
        canvas.drawPath(bow, paint)
        val bow2 = Path().apply {
            moveTo(55f, 20f); quadTo(70f, 5f, 73f, 18f)
            quadTo(68f, 27f, 55f, 22f); close()
        }
        canvas.drawPath(bow2, paint)
        paint.color = deepPink
        canvas.drawCircle(54f, 21f, 3.5f, paint)

        paint.color = cream
        val dress = Path().apply {
            moveTo(38f, 78f); quadTo(55f, 70f, 72f, 78f)
            lineTo(91f, 111f); quadTo(56f, 124f, 21f, 111f); close()
        }
        canvas.drawPath(dress, paint)
        paint.color = pink
        canvas.drawOval(20f, 101f, 92f, 120f, paint)
        paint.color = Color.WHITE
        for (i in 0..4) canvas.drawCircle(29f + i * 13f, 109f, 2.3f, paint)
        paint.color = deepPink
        canvas.drawCircle(56f, 86f, 4f, paint)
        paint.color = pink
        canvas.drawCircle(56f, 86f, 2f, paint)

        paint.color = deepPink
        val heart = Path().apply {
            moveTo(88f, 26f); cubicTo(79f, 17f, 74f, 29f, 88f, 37f)
            cubicTo(102f, 28f, 97f, 17f, 88f, 26f); close()
        }
        canvas.drawPath(heart, paint)
        canvas.restore()
    }

    override fun performClick(): Boolean {
        super.performClick()
        invalidate()
        return true
    }
}
