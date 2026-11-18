package uz.mau.pet

import android.content.Context
import android.content.SharedPreferences

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

private const val PREFS = "mau"
private const val KEY_GEMINI = "gemini_key"

fun Context.prefs(): SharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
fun Context.geminiKey(): String = prefs().getString(KEY_GEMINI, "") ?: ""
fun Context.saveGeminiKey(key: String) {
    prefs().edit().putString(KEY_GEMINI, key.trim()).apply()
}
