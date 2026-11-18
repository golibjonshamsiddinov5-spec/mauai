package uz.mau.pet

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    // Modelni shu yerda almashtirish mumkin.
    const val MODEL = "gemini-3.5-flash-lite"

    class Reply(val text: String?, val error: String?)

    /** history: ("user" yoki "model", matn) juftliklari, oxirgisi "user" bo'lishi kerak. Fon oqimida chaqiring. */
    fun ask(key: String, rules: String, history: List<Pair<String, String>>): Reply {
        var conn: HttpURLConnection? = null
        try {
            val contents = JSONArray()
            for ((role, text) in history) {
                contents.put(
                    JSONObject()
                        .put("role", role)
                        .put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            }
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", rules))))
                .put("contents", contents)
                .put("generationConfig", JSONObject().put("maxOutputTokens", 200).put("temperature", 0.9))

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) return Reply(null, "http_$code")

            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val parts = JSONObject(json)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
            val text = sb.toString().trim()
            return if (text.isEmpty()) Reply(null, "empty") else Reply(text, null)
        } catch (e: IOException) {
            return Reply(null, "network")
        } catch (e: Exception) {
            return Reply(null, "empty")
        } finally {
            conn?.disconnect()
        }
    }

    fun errorCopy(code: String?): String = when (code) {
        "no_key" -> "Gemini kaliti kiritilmagan. Mau ilovasida kiriting."
        "http_400" -> "Kalit yoki soʻrov notoʻgʻri. Kalitni tekshiring."
        "http_403" -> "Kalit ruxsat bermadi. Kalitni tekshiring."
        "http_429" -> "Mau charchadi. Birozdan keyin yozing."
        "network" -> "Internet yoʻq shekilli. Mau eshitmadi."
        else -> "Mau eshitmadi. Yana yozing."
    }
}
