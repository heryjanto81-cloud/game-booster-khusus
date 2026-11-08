package com.redmagic.d8400booster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: CrosshairView? = null
    private var fpsHudView: TextView? = null

    private var isRunning = false
    private var frameCount = 0
    private var lastTimeNanos = 0L

    companion object {
        const val EXTRA_SHOW_CROSSHAIR = "SHOW_CROSSHAIR"
        const val EXTRA_CROSSHAIR_STYLE = "CROSSHAIR_STYLE"
        const val EXTRA_CROSSHAIR_COLOR = "CROSSHAIR_COLOR"
        const val EXTRA_CROSSHAIR_SIZE = "CROSSHAIR_SIZE"
        const val EXTRA_SHOW_FPS = "SHOW_FPS"
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return
            frameCount++
            if (lastTimeNanos == 0L) {
                lastTimeNanos = frameTimeNanos
            }
            val elapsedMs = (frameTimeNanos - lastTimeNanos) / 1_000_000
            if (elapsedMs >= 1000) {
                val fps = ((frameCount * 1000) / elapsedMs).coerceIn(1, 144)
                fpsHudView?.text = "⚡ D8400-ULTRA | $fps FPS | A725 3.25GHz"
                frameCount = 0
                lastTimeNanos = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val showCrosshair = intent?.getBooleanExtra(EXTRA_SHOW_CROSSHAIR, false) ?: false
        val style = intent?.getIntExtra(EXTRA_CROSSHAIR_STYLE, 0) ?: 0
        val colorHex = intent?.getStringExtra(EXTRA_CROSSHAIR_COLOR) ?: "#00FF66"
        val sizeScale = intent?.getFloatExtra(EXTRA_CROSSHAIR_SIZE, 1.0f) ?: 1.0f
        val showFps = intent?.getBooleanExtra(EXTRA_SHOW_FPS, false) ?: false

        updateCrosshair(showCrosshair, style, Color.parseColor(colorHex), sizeScale)
        updateFpsHud(showFps)

        if (!showCrosshair && !showFps) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun updateCrosshair(show: Boolean, style: Int, color: Int, sizeScale: Float) {
        if (show) {
            if (crosshairView == null) {
                crosshairView = CrosshairView(this, style, color, sizeScale)
                val params = WindowManager.LayoutParams(
                    200, 200,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                windowManager?.addView(crosshairView, params)
            } else {
                crosshairView?.updateConfig(style, color, sizeScale)
            }
        } else {
            crosshairView?.let {
                windowManager?.removeView(it)
                crosshairView = null
            }
        }
    }

    private fun updateFpsHud(show: Boolean) {
        if (show) {
            if (fpsHudView == null) {
                fpsHudView = TextView(this).apply {
                    text = "⚡ D8400-ULTRA | 120 FPS | A725 3.25GHz"
                    setTextColor(Color.parseColor("#00F0FF"))
                    setBackgroundColor(Color.parseColor("#CC090A0F"))
                    textSize = 11f
                    setPadding(26, 10, 26, 10)
                }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 40
                    y = 24
                }
                windowManager?.addView(fpsHudView, params)
                isRunning = true
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        } else {
            isRunning = false
            fpsHudView?.let {
                windowManager?.removeView(it)
                fpsHudView = null
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "red_dimensity_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "RedDimensity Game Space Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
            val notif = Notification.Builder(this, channelId)
                .setContentTitle("RedDimensity 8400-Ultra Active")
                .setContentText("Crosshair & Real-Time FPS HUD sedang aktif")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
            startForeground(101, notif)
        }
    }

    override fun onDestroy() {
        isRunning = false
        crosshairView?.let { windowManager?.removeView(it) }
        fpsHudView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class CrosshairView(
    context: Context,
    private var style: Int,
    private var color: Int,
    private var scale: Float
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3.5f
    }

    fun updateConfig(newStyle: Int, newColor: Int, newScale: Float) {
        style = newStyle
        color = newColor
        scale = newScale
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = color
        paint.strokeWidth = 3.5f * scale

        when (style) {
            0 -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, 3.5f * scale, paint)
                paint.style = Paint.Style.STROKE
                val len = 24f * scale
                val gap = 7f * scale
                canvas.drawLine(cx - len, cy, cx - gap, cy, paint)
                canvas.drawLine(cx + gap, cy, cx + len, cy, paint)
                canvas.drawLine(cx, cy - len, cx, cy - gap, paint)
                canvas.drawLine(cx, cy + gap, cx, cy + len, paint)
            }
            1 -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, 3.5f * scale, paint)
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(cx, cy, 20f * scale, paint)
            }
            2 -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, 5.5f * scale, paint)
            }
            3 -> {
                paint.style = Paint.Style.STROKE
                val path = Path().apply {
                    moveTo(cx - 16f * scale, cy + 10f * scale)
                    lineTo(cx, cy - 8f * scale)
                    lineTo(cx + 16f * scale, cy + 10f * scale)
                }
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy + 4f * scale, 3f * scale, paint)
            }
        }
    }
}
