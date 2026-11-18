package uz.mau.pet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var keyField: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        fun text(s: String, size: Float, bold: Boolean = false, color: String = "#1C2A30"): TextView =
            TextView(this).apply {
                this.text = s
                textSize = size
                setTextColor(Color.parseColor(color))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, dp(10))
            }

        fun button(s: String, onClick: () -> Unit): Button =
            Button(this).apply {
                this.text = s
                setOnClickListener { onClick() }
            }

        col.addView(text("Mau", 30f, bold = true))
        col.addView(text("Telefon ekrani ustida yuradigan mushukcha. U bilan Gemini orqali gaplashasiz.", 15f, color = "#586A72"))

        col.addView(text("1. Gemini API kaliti", 17f, bold = true))
        col.addView(text("Kalitni aistudio.google.com/apikey sahifasidan olasiz. U faqat shu telefonda saqlanadi.", 14f, color = "#586A72"))
        keyField = EditText(this).apply {
            hint = "AIza..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(geminiKey())
        }
        col.addView(keyField)
        col.addView(button("Kalitni saqlash") {
            saveGeminiKey(keyField.text.toString())
            Toast.makeText(this, "Saqlandi", Toast.LENGTH_SHORT).show()
            refresh()
        })

        col.addView(text("2. Ekran ustida koʻrsatish", 17f, bold = true).apply { setPadding(0, dp(18), 0, dp(10)) })
        col.addView(text("Mushukcha boshqa ilovalar ustida yurishi uchun ruxsat kerak.", 14f, color = "#586A72"))
        col.addView(button("Ruxsat berish") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        })

        col.addView(text("3. Ishga tushirish", 17f, bold = true).apply { setPadding(0, dp(18), 0, dp(10)) })
        col.addView(button("Mushukchani chiqarish") { startPet() })
        col.addView(button("Toʻxtatish") {
            startService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_STOP))
            window.decorView.postDelayed({ refresh() }, 400)
        })

        status = text("", 14f, color = "#586A72").apply { setPadding(0, dp(18), 0, 0) }
        col.addView(status)

        val scroll = ScrollView(this)
        scroll.addView(col, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun startPet() {
        saveGeminiKey(keyField.text.toString())
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Avval ekran ustida koʻrsatishga ruxsat bering", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        startForegroundService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_START))
        window.decorView.postDelayed({ refresh() }, 600)
    }

    private fun refresh() {
        val hasKey = geminiKey().isNotEmpty()
        val perm = Settings.canDrawOverlays(this)
        status.text = "Kalit: " + (if (hasKey) "bor" else "yoʻq") +
            "\nEkran ustida koʻrsatish: " + (if (perm) "ruxsat berilgan" else "ruxsat yoʻq") +
            "\nMau: " + (if (OverlayService.running) "yuribdi" else "toʻxtagan")
    }
}
