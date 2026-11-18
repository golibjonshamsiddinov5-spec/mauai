package uz.mau.pet

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/** Mushukchaga tegilganda chiqadigan kichik yozish oynasi. Javobni mushukcha pufakda aytadi. */
class ChatActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundColor(0x55000000)
        root.setOnClickListener { finish() }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpf(18f)
            }
        }

        val input = EditText(this).apply {
            hint = "Mau bilan gaplash"
            setHintTextColor(Color.parseColor("#7A8C93"))
            setTextColor(Color.parseColor("#1C2A30"))
            textSize = 16f
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = null
        }
        val send = Button(this).apply { text = "Yubor" }

        fun submit() {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                startService(
                    Intent(this@ChatActivity, OverlayService::class.java)
                        .setAction(OverlayService.ACTION_ASK)
                        .putExtra(OverlayService.EXTRA_TEXT, t)
                )
            }
            finish()
        }
        send.setOnClickListener { submit() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { submit(); true } else false
        }

        panel.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(send, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        lp.setMargins(dp(12), 0, dp(12), dp(12))
        root.addView(panel, lp)
        setContentView(root)
        input.requestFocus()
    }
}
