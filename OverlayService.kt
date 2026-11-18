package uz.mau.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import kotlin.random.Random

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "uz.mau.pet.START"
        const val ACTION_STOP = "uz.mau.pet.STOP"
        const val ACTION_ASK = "uz.mau.pet.ASK"
        const val EXTRA_TEXT = "text"
        private const val CHANNEL = "mau"
        private const val NOTIF_ID = 1
        private const val BOTTOM_MARGIN_DP = 40 // navigatsiya paneli ustida turishi uchun, kerak bo'lsa o'zgartiring

        @Volatile
        var running = false
    }

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var cat: CatView? = null
    private var catParams: WindowManager.LayoutParams? = null
    private var bubble: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleVisible = false

    private var x = 0f
    private var lastX = -1
    private var dir = 1
    private var state = CatState.WALK
    private var until = 0L
    private var lastTick = 0L
    private var startMs = 0L
    private var busy = false
    private val history = ArrayList<Pair<String, String>>()

    private val hideBubbleRunnable = Runnable { hideBubble() }
    private val tick = object : Runnable {
        override fun run() {
            step()
            handler.postDelayed(this, 33)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startAsForeground()
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (cat == null) createOverlay()
                running = true
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_ASK -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                if (cat != null) ask(text)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        cat?.let { runCatching { wm.removeView(it) } }
        if (bubbleVisible) bubble?.let { runCatching { wm.removeView(it) } }
        cat = null
        bubble = null
        bubbleVisible = false
        super.onDestroy()
    }

    // ---------- Bildirishnoma (foreground xizmat) ----------

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Mau", NotificationManager.IMPORTANCE_MIN))
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Mau ekranda yuribdi")
            .setContentText("Toʻxtatish uchun bosing")
            .setContentIntent(stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    // ---------- Overlay oynalar ----------

    private fun createOverlay() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val w = dp(130)
        val h = (w * 0.7143f * 1.5f).toInt()
        val view = CatView(this)
        view.setOnClickListener { onTap() }
        val p = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.BOTTOM or Gravity.START
        p.x = 0
        p.y = dp(BOTTOM_MARGIN_DP)
        wm.addView(view, p)
        cat = view
        catParams = p

        val now = SystemClock.uptimeMillis()
        startMs = now
        lastTick = now
        setState(CatState.WALK, 5000)
        handler.post(tick)
    }

    private fun createBubble(): TextView {
        val tv = TextView(this)
        tv.setTextColor(Color.parseColor("#1C2A30"))
        tv.textSize = 15f
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setPadding(dp(14), dp(10), dp(14), dp(10))
        tv.maxWidth = (resources.displayMetrics.widthPixels * 0.7f).toInt()
        tv.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dpf(16f)
        }
        tv.elevation = dpf(6f)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.BOTTOM or Gravity.START
        p.y = dp(BOTTOM_MARGIN_DP) + (dp(130) * 0.7143f).toInt() + dp(6)
        bubbleParams = p
        bubble = tv
        return tv
    }

    private fun showBubble(text: String, ttl: Long) {
        if (cat == null) return
        val b = bubble ?: createBubble()
        b.text = text
        if (!bubbleVisible) {
            wm.addView(b, bubbleParams)
            bubbleVisible = true
        }
        handler.removeCallbacks(hideBubbleRunnable)
        if (ttl > 0) handler.postDelayed(hideBubbleRunnable, ttl)
        b.post { placeBubble() }
    }

    private fun hideBubble() {
        val b = bubble ?: return
        if (bubbleVisible) {
            runCatching { wm.removeView(b) }
            bubbleVisible = false
        }
    }

    private fun placeBubble() {
        val b = bubble ?: return
        val p = bubbleParams ?: return
        if (!bubbleVisible || b.width == 0) return
        val screenW = resources.displayMetrics.widthPixels
        val cx = x + dp(130) / 2f
        val left = (cx - b.width / 2f).coerceIn(dp(8).toFloat(), (screenW - b.width - dp(8)).toFloat().coerceAtLeast(dp(8).toFloat()))
        if (p.x != left.toInt()) {
            p.x = left.toInt()
            runCatching { wm.updateViewLayout(b, p) }
        }
    }

    // ---------- Harakat ----------

    private fun setState(s: CatState, durMs: Long) {
        state = s
        until = SystemClock.uptimeMillis() + durMs
    }

    private fun nextState() {
        if (busy || bubbleVisible) {
            setState(CatState.SIT, 2000)
            return
        }
        val r = Random.nextFloat()
        when {
            r < 0.55f -> {
                if (Random.nextFloat() < 0.4f) dir = -dir
                setState(CatState.WALK, Random.nextLong(4000, 9000))
            }
            r < 0.82f -> setState(CatState.SIT, Random.nextLong(3000, 6000))
            else -> setState(CatState.SLEEP, Random.nextLong(10000, 18000))
        }
    }

    private fun step() {
        val c = cat ?: return
        val p = catParams ?: return
        val now = SystemClock.uptimeMillis()
        val dt = minOf(0.05f, (now - lastTick) / 1000f)
        lastTick = now

        val max = (resources.displayMetrics.widthPixels - p.width).coerceAtLeast(0).toFloat()
        if (state == CatState.WALK) {
            x += dir * dpf(45f) * dt
            if (x <= 0f) { x = 0f; dir = 1 }
            if (x >= max) { x = max; dir = -1 }
        } else if (x > max) {
            x = max
        }

        c.state = state
        c.facingRight = dir == 1
        c.time = (now - startMs) / 1000f

        val xi = x.toInt()
        if (xi != lastX) {
            lastX = xi
            p.x = xi
            runCatching { wm.updateViewLayout(c, p) }
        }
        c.invalidate()
        if (now > until) nextState()
        placeBubble()
    }

    // ---------- Gaplashish ----------

    private fun onTap() {
        setState(CatState.PURR, 3000)
        if (!busy) {
            val lines = listOf("Prrr…", "Mmm, yoqdi.", "Mau!")
            showBubble(lines.random(), 2500)
        }
        val i = Intent(this, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun rules(): String {
        val doing = when (state) {
            CatState.WALK -> "ekran pastida aylanib yuribsan"
            CatState.SIT -> "ekran pastida oʻtiribsan"
            CatState.SLEEP -> "uxlayotgan eding, hozir uygʻonding"
            CatState.PURR -> "siypalanib xirillayapsan"
        }
        return "Sen Mau ismli kichkina uy mushukchasan va telefon ekranining pastida yurasan. " +
            "Foydalanuvchi bilan doʻstona, biroz hazil aralash gaplash. " +
            "1-2 ta qisqa gap bilan javob ber (jami 160 belgidan oshmasin), oddiy matn, markdown va emoji yoʻq. " +
            "Ba'zan 'mau' yoki 'prrr' deb qoʻy, lekin har gapda emas. " +
            "Foydalanuvchi qaysi tilda yozsa, shu tilda javob ber; asosiy til lotin alifbosidagi oʻzbek tili. " +
            "Dunyoga mushuk koʻzi bilan qara. Hozir sen $doing. " +
            "Bilmagan narsani toʻqima, mushuk uslubida ochiq ayt. Xavfli narsalarga yordam berma, yumshoq rad et."
    }

    private fun ask(text: String) {
        val q = text.trim()
        if (busy || q.isEmpty()) return
        val key = geminiKey()
        if (key.isEmpty()) {
            showBubble(Gemini.errorCopy("no_key"), 7000)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        busy = true
        if (state == CatState.SLEEP) setState(CatState.SIT, 8000)
        showBubble("Mau…", 0)

        history.add("user" to q)
        while (history.size > 12) history.removeAt(0)
        while (history.isNotEmpty() && history[0].first != "user") history.removeAt(0)
        val snapshot = ArrayList(history)
        val sys = rules()

        Thread {
            val reply = Gemini.ask(key, sys, snapshot)
            handler.post {
                busy = false
                if (cat == null) return@post
                val t = reply.text
                if (t != null) {
                    history.add("model" to t)
                    showBubble(t, 8000L + t.length * 50L)
                    until = maxOf(until, SystemClock.uptimeMillis() + 5000)
                } else {
                    if (history.isNotEmpty()) history.removeAt(history.size - 1)
                    showBubble(Gemini.errorCopy(reply.error), 7000)
                }
            }
        }.start()
    }
}
